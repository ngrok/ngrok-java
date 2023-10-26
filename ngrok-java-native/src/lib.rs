use async_trait::async_trait;
use bytes::Bytes;
use com_ngrok::{
    ComNgrokEdgeBuilder, ComNgrokHttpBuilder, ComNgrokHttpHeader, ComNgrokNativeEdgeConnection,
    ComNgrokNativeEdgeForwarder, ComNgrokNativeEdgeListener, ComNgrokNativeEndpointConnection,
    ComNgrokNativeHttpForwarder, ComNgrokNativeHttpListener, ComNgrokNativeSession,
    ComNgrokNativeSessionClass, ComNgrokNativeTcpForwarder, ComNgrokNativeTcpListener,
    ComNgrokNativeTlsForwarder, ComNgrokNativeTlsListener, ComNgrokRuntimeLogger,
    ComNgrokSessionBuilder, ComNgrokSessionClientInfo, ComNgrokSessionCommandHandler,
    ComNgrokSessionHeartbeatHandler, ComNgrokTcpBuilder, ComNgrokTlsBuilder, IOException,
    IOExceptionErr, JavaNetUrl, JavaUtilList, JavaUtilMap, JavaUtilOptional,
};
use futures::TryStreamExt;
use once_cell::sync::OnceCell;
use std::{collections::HashMap, str::FromStr, sync::MutexGuard, time::Duration};
use tokio::{io::AsyncReadExt, io::AsyncWriteExt, runtime::Runtime};
use tracing::{level_filters::LevelFilter, Level};
use tracing_subscriber::{prelude::__tracing_subscriber_SubscriberExt, util::SubscriberInitExt};
use url::Url;

use jaffi_support::{
    jni::{
        objects::{GlobalRef, JByteBuffer, JObject, JString, JValue},
        JNIEnv, JavaVM,
    },
    Error,
};

use ngrok::{
    config::{
        HttpTunnelBuilder, LabeledTunnelBuilder, OauthOptions, OidcOptions, ProxyProto, Scheme,
        TcpTunnelBuilder, TlsTunnelBuilder,
    },
    conn::ConnInfo,
    forwarder::Forwarder,
    prelude::{EdgeConnInfo, EndpointConnInfo, ForwarderBuilder, TunnelBuilder},
    session::{CommandHandler, HeartbeatHandler, Restart, Stop, Update},
    tunnel::{
        AcceptError, EdgeInfo, EndpointInfo, HttpTunnel, LabeledTunnel, TcpTunnel, TlsTunnel,
        TunnelCloser, TunnelInfo,
    },
    EdgeConn, EndpointConn, Error as NError, Session,
};

#[allow(clippy::all)]
#[allow(dead_code)]
mod com_ngrok {
    include!(concat!(env!("OUT_DIR"), "/generated_jaffi.rs"));
}

static RT: OnceCell<Runtime> = OnceCell::new();
static JVM: OnceCell<JavaVM> = OnceCell::new();
static LOGGER: OnceCell<GlobalRef> = OnceCell::new();

struct RuntimeRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> com_ngrok::RuntimeRs<'local> for RuntimeRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn init(
        &self,
        _class: com_ngrok::ComNgrokRuntimeClass<'local>,
        logger: ComNgrokRuntimeLogger<'local>,
    ) {
        match Runtime::new() {
            Ok(rt) => {
                RT.get_or_init(|| rt);

                let jvm = self.env.get_java_vm().expect("cannot get jvm");
                JVM.get_or_init(|| jvm);

                let logref = self
                    .env
                    .new_global_ref(logger)
                    .expect("cannot get logger ref");
                LOGGER.get_or_init(|| logref);

                let log_lvl: Level =
                    Level::from_str(&logger.get_level(self.env)).expect("invalid log level");
                let level_filter: LevelFilter = log_lvl.into();
                tracing_subscriber::registry()
                    .with(TracingLoggingLayer)
                    .with(level_filter)
                    .try_init()
                    .expect("cannot init logging");
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not raise exception");
            }
        }
    }
}

struct TracingLoggingLayer;

impl<S> tracing_subscriber::Layer<S> for TracingLoggingLayer
where
    S: tracing::Subscriber,
{
    fn on_event(
        &self,
        event: &tracing::Event<'_>,
        _ctx: tracing_subscriber::layer::Context<'_, S>,
    ) {
        let mut visitor = TracingEventVisitor {
            ..Default::default()
        };
        event.record(&mut visitor);

        let jvm = JVM.get().expect("no jvm");
        let jenv = jvm
            .attach_current_thread_as_daemon()
            .expect("cannot attach");

        let logref = LOGGER.get().expect("no logger");
        let logger = ComNgrokRuntimeLogger::from(logref.as_obj());
        logger.log(
            jenv,
            event.metadata().level().to_string(),
            event.metadata().target().to_string(),
            visitor.message,
        );
    }
}

#[derive(Default)]
struct TracingEventVisitor {
    message: String,
}

impl tracing::field::Visit for TracingEventVisitor {
    // everything currently used comes through as a fmt debug
    fn record_debug(&mut self, field: &tracing::field::Field, value: &dyn std::fmt::Debug) {
        if field.name() == "message" {
            self.message = format!("{value:?}");
        }
    }
}

trait JNIExt<'local> {
    fn get_env(&self) -> &JNIEnv<'local>;

    fn set_native<J, R>(&self, this: J, target: R)
    where
        J: Into<JObject<'local>>,
        R: Send + 'static,
    {
        self.get_env()
            .set_rust_field(this, "native_address", target)
            .expect("cannot set native address")
    }

    fn get_native<'a, J, R>(&'a self, this: J) -> MutexGuard<'a, R>
    where
        'local: 'a,
        J: Into<JObject<'local>>,
        R: Send + 'static,
    {
        self.get_env()
            .get_rust_field(this, "native_address")
            .expect("cannot get native value")
    }

    fn take_native<J, R>(&self, this: J) -> R
    where
        J: Into<JObject<'local>>,
        R: Send + 'static,
    {
        self.get_env()
            .take_rust_field(this, "native_address")
            .expect("cannot take native value")
    }

    fn as_string(&self, jstr: JString) -> Option<String> {
        if jstr.is_null() {
            None
        } else {
            Some(
                self.get_env()
                    .get_string(jstr)
                    .expect("could not convert to string")
                    .into(),
            )
        }
    }
}

impl<'local> JavaUtilList<'local> {
    fn size(self, env: JNIEnv<'local>) -> i32 {
        env.call_method(self, "size", "()I", &[])
            .and_then(|o| o.i())
            .expect("could not get list size")
    }

    fn get(self, env: JNIEnv<'local>, idx: i32) -> JObject<'local> {
        env.call_method(self, "get", "(I)Ljava/lang/Object;", &[JValue::Int(idx)])
            .and_then(|o| o.l())
            .expect("could not get list item")
    }

    fn get_string(self, env: JNIEnv<'local>, idx: i32) -> String {
        env.get_string(JString::from(self.get(env, idx)))
            .expect("could not convert to string")
            .into()
    }
}

impl<'local> JavaNetUrl<'local> {
    fn as_string(self, env: JNIEnv<'local>) -> String {
        env.call_method(self, "toString", "()Ljava/lang/String;", &[])
            .and_then(|o| o.l())
            .map(JString::from)
            .and_then(|o| env.get_string(o))
            .expect("could not convert url to string")
            .into()
    }
}

impl<'local> JavaUtilOptional<'local> {
    fn of_string(self, env: JNIEnv<'local>) -> Option<String> {
        if self.is_present(env) {
            let s: String = self
                .get(env)
                .map(JString::from)
                .and_then(|o| env.get_string(o))
                .expect("could not convert url to string")
                .into();
            Some(s)
        } else {
            None
        }
    }

    fn of_double(self, env: JNIEnv<'local>) -> Option<f64> {
        if self.is_present(env) {
            let d = self
                .get(env)
                .and_then(|o| env.call_method(o, "doubleValue", "()D", &[]))
                .and_then(|o| o.d())
                .expect("could not get double");
            Some(d)
        } else {
            None
        }
    }

    fn of_duration_ms(self, env: JNIEnv<'local>) -> Option<Duration> {
        if self.is_present(env) {
            let d = self
                .get(env)
                .and_then(|o| env.call_method(o, "toMillis", "()J", &[]))
                .and_then(|o| o.j())
                .expect("cannot get duration millis");
            let du: u64 = d.try_into().expect("cannot convert to unsigned");
            Some(Duration::from_millis(du))
        } else {
            None
        }
    }

    fn get(
        self,
        env: JNIEnv<'local>,
    ) -> Result<JObject<'local>, jaffi_support::jni::errors::Error> {
        env.call_method(self, "get", "()Ljava/lang/Object;", &[])
            .and_then(|o| o.l())
    }

    fn is_present(self, env: JNIEnv<'local>) -> bool {
        env.call_method(self, "isPresent", "()Z", &[])
            .and_then(|o| o.z())
            .expect("could not call isPresent")
    }
}

fn io_exc<E: ToString>(e: E) -> Error<IOExceptionErr> {
    Error::new(IOExceptionErr::IOException(IOException), e.to_string())
}

fn io_exc_err<T, E: ToString>(e: E) -> Result<T, Error<IOExceptionErr>> {
    Err(io_exc(e))
}

fn ngrok_exc<E: ngrok::Error>(e: E) -> Error<IOExceptionErr> {
    match e.error_code() {
        Some(code) => io_exc(format!("{}\n\n{}", code, e.msg())),
        None => io_exc(e.msg()),
    }
}

fn ngrok_exc_err<T, E: ngrok::Error>(e: E) -> Result<T, Error<IOExceptionErr>> {
    Err(ngrok_exc(e))
}

fn accept_exc_err<T>(e: AcceptError) -> Result<T, Error<IOExceptionErr>> {
    match e {
        AcceptError::Reconnect(err) => match err.error_code() {
            Some(code) => io_exc_err(format!("{}\n\n{}", code, err.msg())),
            None => io_exc_err(err.msg()),
        },
        AcceptError::Transport(err) => io_exc_err(err),
        _ => io_exc_err(e),
    }
}

struct CommandHandlerCallback {
    cbk: GlobalRef,
}

impl CommandHandlerCallback {
    fn from(env: JNIEnv<'_>, obj: ComNgrokSessionCommandHandler) -> Self {
        CommandHandlerCallback {
            cbk: env
                .new_global_ref(obj)
                .expect("cannot get global reference"),
        }
    }
}

#[async_trait]
impl CommandHandler<Stop> for CommandHandlerCallback {
    async fn handle_command(&self, _req: Stop) -> Result<(), String> {
        let jvm = JVM.get().expect("no jvm");
        let jenv = jvm.attach_current_thread().expect("cannot attach");

        let lcbk = ComNgrokSessionCommandHandler::from(self.cbk.as_obj());
        lcbk.on_command(*jenv);
        Ok(())
    }
}

#[async_trait]
impl CommandHandler<Restart> for CommandHandlerCallback {
    async fn handle_command(&self, _req: Restart) -> Result<(), String> {
        let jvm = JVM.get().expect("no jvm");
        let jenv = jvm.attach_current_thread().expect("cannot attach");

        let lcbk = ComNgrokSessionCommandHandler::from(self.cbk.as_obj());
        lcbk.on_command(*jenv);
        Ok(())
    }
}

#[async_trait]
impl CommandHandler<Update> for CommandHandlerCallback {
    async fn handle_command(&self, _req: Update) -> Result<(), String> {
        let jvm = JVM.get().expect("no jvm");
        let jenv = jvm.attach_current_thread().expect("cannot attach");

        let lcbk = ComNgrokSessionCommandHandler::from(self.cbk.as_obj());
        lcbk.on_command(*jenv);
        Ok(())
    }
}

struct HeartbeatCallback {
    cbk: GlobalRef,
}

impl HeartbeatCallback {
    fn from(env: JNIEnv<'_>, obj: ComNgrokSessionHeartbeatHandler) -> Self {
        HeartbeatCallback {
            cbk: env
                .new_global_ref(obj)
                .expect("cannot get global reference"),
        }
    }
}

#[async_trait]
impl HeartbeatHandler for HeartbeatCallback {
    async fn handle_heartbeat(
        &self,
        latency: Option<Duration>,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let jvm = JVM.get().expect("no jvm");
        let jenv = jvm.attach_current_thread()?;

        let lcbk = ComNgrokSessionHeartbeatHandler::from(self.cbk.as_obj());
        match latency {
            Some(latency) => lcbk.heartbeat(*jenv, i64::try_from(latency.as_millis())?),
            None => lcbk.timeout(*jenv),
        }
        Ok(())
    }
}

struct NativeSessionRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> NativeSessionRsImpl<'local> {
    fn tcp_builder(
        &self,
        sess: MutexGuard<Session>,
        jttb: ComNgrokTcpBuilder<'local>,
    ) -> Result<TcpTunnelBuilder, Error<IOExceptionErr>> {
        let mut bldr = sess.tcp_endpoint();

        let jeb = jttb.as_com_ngrok_endpoint_builder();
        let jmb = jeb.as_com_ngrok_metadata_builder();

        // from Tunnel.Builder
        if let Some(metadata) = jmb.get_metadata(self.env).of_string(self.env) {
            bldr.metadata(metadata);
        }

        // from EndpointTunnel.Builder
        let allow_cidr = jeb.get_allow_cidr(self.env);
        for i in 0..allow_cidr.size(self.env) {
            bldr.allow_cidr(allow_cidr.get_string(self.env, i));
        }

        let deny_cidr = jeb.get_deny_cidr(self.env);
        for i in 0..deny_cidr.size(self.env) {
            bldr.deny_cidr(deny_cidr.get_string(self.env, i));
        }

        bldr.proxy_proto(ProxyProto::from(jeb.get_proxy_proto_version(self.env)));

        if let Some(forwards_to) = jeb.get_forwards_to(self.env).of_string(self.env) {
            bldr.forwards_to(forwards_to);
        }

        // from TcpTunnel.Builder
        if let Some(remote_addr) = jttb.get_remote_address(self.env).of_string(self.env) {
            bldr.remote_addr(remote_addr);
        }

        Ok(bldr)
    }

    fn tls_builder(
        &self,
        sess: MutexGuard<Session>,
        jttb: ComNgrokTlsBuilder<'local>,
    ) -> Result<TlsTunnelBuilder, Error<IOExceptionErr>> {
        let mut bldr = sess.tls_endpoint();

        let jeb = jttb.as_com_ngrok_endpoint_builder();
        let jmb = jeb.as_com_ngrok_metadata_builder();

        // from Tunnel.Builder
        if let Some(metadata) = jmb.get_metadata(self.env).of_string(self.env) {
            bldr.metadata(metadata);
        }

        // from EndpointTunnel.Builder
        let allow_cidr = jeb.get_allow_cidr(self.env);
        for i in 0..allow_cidr.size(self.env) {
            bldr.allow_cidr(allow_cidr.get_string(self.env, i));
        }

        let deny_cidr = jeb.get_deny_cidr(self.env);
        for i in 0..deny_cidr.size(self.env) {
            bldr.deny_cidr(deny_cidr.get_string(self.env, i));
        }

        bldr.proxy_proto(ProxyProto::from(jeb.get_proxy_proto_version(self.env)));

        if let Some(forwards_to) = jeb.get_forwards_to(self.env).of_string(self.env) {
            bldr.forwards_to(forwards_to);
        }

        // from TlsTunnel.Builder
        if let Some(domain) = jttb.get_domain(self.env).of_string(self.env) {
            bldr.domain(domain);
        }

        let mtls = jttb.get_mutual_tlsca(self.env);
        if !mtls.is_null() {
            let mtls_data = mtls.as_slice(&self.env).expect("cannot get mtls data");
            bldr.mutual_tlsca(Bytes::copy_from_slice(&mtls_data));
        }

        match (
            jttb.get_termination_cert_pem(self.env),
            jttb.get_termination_key_pem(self.env),
        ) {
            (cert, key) if !cert.is_null() && !key.is_null() => {
                let cert_pem_data = cert.as_slice(&self.env).expect("cannot get cert data");
                let key_pem_data = key.as_slice(&self.env).expect("cannot get key data");
                bldr.termination(
                    Bytes::copy_from_slice(&cert_pem_data),
                    Bytes::copy_from_slice(&key_pem_data),
                );
            }
            (cert, key) if cert.is_null() && key.is_null() => {}
            _ => return io_exc_err("requires both terminationCertPEM and terminationKeyPEM"),
        }

        Ok(bldr)
    }

    fn http_builder(
        &self,
        sess: MutexGuard<Session>,
        jhtb: ComNgrokHttpBuilder<'local>,
    ) -> Result<HttpTunnelBuilder, Error<IOExceptionErr>> {
        let mut bldr = sess.http_endpoint();

        let jeb = jhtb.as_com_ngrok_endpoint_builder();
        let jmb = jeb.as_com_ngrok_metadata_builder();

        // from Tunnel.Builder
        if let Some(metadata) = jmb.get_metadata(self.env).of_string(self.env) {
            bldr.metadata(metadata);
        }

        // from EndpointTunnel.Builder
        let allow_cidr = jeb.get_allow_cidr(self.env);
        for i in 0..allow_cidr.size(self.env) {
            bldr.allow_cidr(allow_cidr.get_string(self.env, i));
        }

        let deny_cidr = jeb.get_deny_cidr(self.env);
        for i in 0..deny_cidr.size(self.env) {
            bldr.deny_cidr(deny_cidr.get_string(self.env, i));
        }

        bldr.proxy_proto(ProxyProto::from(jeb.get_proxy_proto_version(self.env)));

        if let Some(forwards_to) = jeb.get_forwards_to(self.env).of_string(self.env) {
            bldr.forwards_to(forwards_to);
        }

        // from HttpTunnel.Builder
        if let Some(scheme) = jhtb.get_scheme_name(self.env).of_string(self.env) {
            let scheme = Scheme::from_str(scheme.as_str()).map_err(io_exc)?;
            bldr.scheme(scheme);
        }

        if let Some(domain) = jhtb.get_domain(self.env).of_string(self.env) {
            bldr.domain(domain);
        }

        let mtls = jhtb.get_mutual_tlsca(self.env);
        if !mtls.is_null() {
            let slice = mtls.as_slice(&self.env).expect("cannot get mtls data");
            bldr.mutual_tlsca(Bytes::copy_from_slice(&slice));
        }

        if jhtb.is_compression(self.env) {
            bldr.compression();
        }

        if jhtb.is_websocket_tcp_conversion(self.env) {
            bldr.websocket_tcp_conversion();
        }

        if let Some(circuit_breaker) = jhtb.get_circuit_breaker(self.env).of_double(self.env) {
            bldr.circuit_breaker(circuit_breaker);
        }

        let request_headers = jhtb.get_request_headers(self.env);
        for i in 0..request_headers.size(self.env) {
            let header: ComNgrokHttpHeader = request_headers.get(self.env, i).into();
            bldr.request_header(header.get_name(self.env), header.get_value(self.env));
        }

        let response_headers = jhtb.get_response_headers(self.env);
        for i in 0..response_headers.size(self.env) {
            let header: ComNgrokHttpHeader = response_headers.get(self.env, i).into();
            bldr.response_header(header.get_name(self.env), header.get_value(self.env));
        }

        let remove_request_headers = jhtb.get_remove_request_headers(self.env);
        for i in 0..remove_request_headers.size(self.env) {
            bldr.remove_request_header(remove_request_headers.get_string(self.env, i));
        }

        let remove_response_headers = jhtb.get_remove_response_headers(self.env);
        for i in 0..remove_response_headers.size(self.env) {
            bldr.remove_response_header(remove_response_headers.get_string(self.env, i));
        }

        let basic_auth = jhtb.get_basic_auth(self.env);
        if !basic_auth.is_null() {
            bldr.basic_auth(
                basic_auth.get_username(self.env),
                basic_auth.get_password(self.env),
            );
        }

        let joauth = jhtb.get_oauth(self.env);
        if !joauth.is_null() {
            let mut oauth = OauthOptions::new(joauth.get_provider(self.env));
            if joauth.has_client_configured(self.env) {
                oauth.client_id(joauth.get_client_id(self.env));
                oauth.client_secret(joauth.get_client_secret(self.env));
            }

            let allow_emails = joauth.get_allow_emails(self.env);
            for i in 0..allow_emails.size(self.env) {
                oauth.allow_email(allow_emails.get_string(self.env, i));
            }

            let allow_domains = joauth.get_allow_domains(self.env);
            for i in 0..allow_domains.size(self.env) {
                oauth.allow_domain(allow_domains.get_string(self.env, i));
            }

            let scopes: JavaUtilList<'_> = joauth.get_scopes(self.env);
            for i in 0..scopes.size(self.env) {
                oauth.scope(scopes.get_string(self.env, i));
            }

            bldr.oauth(oauth);
        }

        let joidc = jhtb.get_oidc(self.env);
        if !joidc.is_null() {
            let mut oidc = OidcOptions::new(
                joidc.get_issuer_url(self.env),
                joidc.get_client_id(self.env),
                joidc.get_client_secret(self.env),
            );

            let allow_emails = joauth.get_allow_emails(self.env);
            for i in 0..allow_emails.size(self.env) {
                oidc.allow_email(allow_emails.get_string(self.env, i));
            }

            let allow_domains = joauth.get_allow_domains(self.env);
            for i in 0..allow_domains.size(self.env) {
                oidc.allow_domain(allow_domains.get_string(self.env, i));
            }

            let scopes: JavaUtilList<'_> = joauth.get_scopes(self.env);
            for i in 0..scopes.size(self.env) {
                oidc.scope(scopes.get_string(self.env, i));
            }

            bldr.oidc(oidc);
        }

        let jwv = jhtb.get_webhook_verification(self.env);
        if !jwv.is_null() {
            bldr.webhook_verification(jwv.get_provider(self.env), jwv.get_secret(self.env));
        }

        Ok(bldr)
    }

    fn edge_builder(
        &self,
        sess: MutexGuard<Session>,
        jltb: ComNgrokEdgeBuilder<'local>,
    ) -> Result<LabeledTunnelBuilder, Error<IOExceptionErr>> {
        let mut bldr = sess.labeled_tunnel();

        let jmb = jltb.as_com_ngrok_metadata_builder();

        // from Tunnel.Builder
        if let Some(metadata) = jmb.get_metadata(self.env).of_string(self.env) {
            bldr.metadata(metadata);
        }

        // from LabeledTunnel.Builder
        let labels = self
            .env
            .get_map(jltb.get_labels(self.env).into())
            .expect("cannot get labels map");
        labels
            .iter()
            .and_then(|iter| {
                for e in iter {
                    let key = self.env.get_string(e.0.into())?;
                    let value = self.env.get_string(e.1.into())?;
                    bldr.label(key, value);
                }
                Ok(())
            })
            .expect("cannot iterate over labels map");

        Ok(bldr)
    }

    fn labels_map(
        &self,
        labels: &HashMap<String, String>,
    ) -> jaffi_support::jni::errors::Result<JavaUtilMap<'local>> {
        let map_class = self.env.find_class("java/util/HashMap")?;
        let map = self.env.new_object(map_class, "()V", &[])?;
        let mmap = self.env.get_map(map)?;
        for (key, value) in labels {
            let jkey = self.env.new_string(key)?;
            let jvalue = self.env.new_string(value)?;
            mmap.put(jkey.into(), jvalue.into())?;
        }
        Ok(map.into())
    }

    fn close_tunnel(
        &self,
        this: ComNgrokNativeSession<'local>,
        tunnel_id: String,
    ) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        rt.block_on(sess.close_tunnel(tunnel_id)).map_err(ngrok_exc)
    }
}

impl<'local> JNIExt<'local> for NativeSessionRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeSessionRs<'local> for NativeSessionRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn connect_native(
        &self,
        _class: ComNgrokNativeSessionClass<'local>,
        jsb: ComNgrokSessionBuilder<'local>,
    ) -> Result<ComNgrokNativeSession<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut bldr = Session::builder();

        bldr.authtoken(jsb.get_authtoken(self.env));

        if let Some(interval) = jsb
            .get_heartbeat_interval(self.env)
            .of_duration_ms(self.env)
        {
            bldr.heartbeat_interval(interval).map_err(io_exc)?;
        }

        if let Some(tolerance) = jsb
            .get_heartbeat_tolerance(self.env)
            .of_duration_ms(self.env)
        {
            bldr.heartbeat_tolerance(tolerance).map_err(io_exc)?;
        }

        let mut session_metadata = String::from("");
        if let Some(metadata) = jsb.get_metadata(self.env).of_string(self.env) {
            session_metadata = metadata.clone();
            bldr.metadata(metadata);
        }

        if let Some(server_addr) = jsb.get_server_addr(self.env).of_string(self.env) {
            bldr.server_addr(server_addr).map_err(io_exc)?;
        }

        let ca_cert = jsb.get_ca_cert(self.env);
        if !ca_cert.is_null() {
            let ca_cert_data = ca_cert
                .as_slice(&self.env)
                .expect("cannot get ca cert data");
            bldr.ca_cert(Bytes::copy_from_slice(&ca_cert_data));
        }

        // TODO: tls_config
        // TODO: connector?

        let stop_obj = jsb.stop_callback(self.env);
        if !stop_obj.is_null() {
            bldr.handle_stop_command(CommandHandlerCallback::from(self.env, stop_obj));
        }

        let restart_obj = jsb.restart_callback(self.env);
        if !restart_obj.is_null() {
            bldr.handle_restart_command(CommandHandlerCallback::from(self.env, restart_obj));
        }

        let update_obj = jsb.update_callback(self.env);
        if !update_obj.is_null() {
            bldr.handle_update_command(CommandHandlerCallback::from(self.env, update_obj));
        }

        let heartbeat_obj = jsb.heartbeat_handler(self.env);
        if !heartbeat_obj.is_null() {
            bldr.handle_heartbeat(HeartbeatCallback::from(self.env, heartbeat_obj));
        }

        let client_infos = jsb.get_client_infos(self.env);
        for i in 0..client_infos.size(self.env) {
            let client_info: ComNgrokSessionClientInfo = client_infos.get(self.env, i).into();
            bldr.client_info(
                client_info.get_type(self.env),
                client_info.get_version(self.env),
                client_info.get_comments(self.env).of_string(self.env),
            );
        }

        match rt.block_on(bldr.connect()) {
            Ok(sess) => {
                let jsess = ComNgrokNativeSession::new_1com_ngrok_native_session(
                    self.env,
                    sess.id(),
                    session_metadata,
                );
                self.set_native(jsess, sess);
                Ok(jsess)
            }
            Err(err) => ngrok_exc_err(err),
        }
    }

    fn listen_tcp(
        &self,
        this: ComNgrokNativeSession<'local>,
        jttb: ComNgrokTcpBuilder<'local>,
    ) -> Result<com_ngrok::ComNgrokNativeTcpListener<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let bldr = self.tcp_builder(sess, jttb)?;

        let tun = rt.block_on(bldr.listen());
        match tun {
            Ok(tun) => {
                let jlistener = ComNgrokNativeTcpListener::new_1com_ngrok_native_tcp_listener(
                    self.env,
                    tun.id().into(),
                    tun.metadata().into(),
                    tun.forwards_to().into(),
                    tun.proto().to_string(),
                    tun.url().into(),
                );
                self.set_native(jlistener, tun);
                Ok(jlistener)
            }
            Err(err) => ngrok_exc_err(err),
        }
    }

    fn forward_tcp(
        &self,
        this: ComNgrokNativeSession<'local>,
        jttb: ComNgrokTcpBuilder<'local>,
        jurl: com_ngrok::JavaNetUrl<'local>,
    ) -> Result<com_ngrok::ComNgrokNativeTcpForwarder<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let bldr = self.tcp_builder(sess, jttb)?;

        let url = Url::parse(jurl.as_string(self.env).as_str()).map_err(io_exc)?;

        let tun = rt.block_on(bldr.listen_and_forward(url));
        match tun {
            Ok(tun) => {
                let jforwarder = ComNgrokNativeTcpForwarder::new_1com_ngrok_native_tcp_forwarder(
                    self.env,
                    tun.id().into(),
                    tun.metadata().into(),
                    tun.forwards_to().into(),
                    tun.proto().to_string(),
                    tun.url().into(),
                );
                self.set_native(jforwarder, tun);
                Ok(jforwarder)
            }
            Err(err) => ngrok_exc_err(err),
        }
    }

    fn listen_tls(
        &self,
        this: ComNgrokNativeSession<'local>,
        jttb: ComNgrokTlsBuilder<'local>,
    ) -> Result<com_ngrok::ComNgrokNativeTlsListener<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let bldr = self.tls_builder(sess, jttb)?;

        let tun = rt.block_on(bldr.listen());
        match tun {
            Ok(tun) => {
                let jlistener = ComNgrokNativeTlsListener::new_1com_ngrok_native_tls_listener(
                    self.env,
                    tun.id().into(),
                    tun.metadata().into(),
                    tun.forwards_to().into(),
                    tun.proto().to_string(),
                    tun.url().into(),
                );
                self.set_native(jlistener, tun);
                Ok(jlistener)
            }
            Err(err) => ngrok_exc_err(err),
        }
    }

    fn forward_tls(
        &self,
        this: ComNgrokNativeSession<'local>,
        jttb: ComNgrokTlsBuilder<'local>,
        jurl: com_ngrok::JavaNetUrl<'local>,
    ) -> Result<com_ngrok::ComNgrokNativeTlsForwarder<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let bldr = self.tls_builder(sess, jttb)?;

        let url = Url::parse(jurl.as_string(self.env).as_str()).map_err(io_exc)?;

        let tun = rt.block_on(bldr.listen_and_forward(url));
        match tun {
            Ok(tun) => {
                let jforwarder = ComNgrokNativeTlsForwarder::new_1com_ngrok_native_tls_forwarder(
                    self.env,
                    tun.id().into(),
                    tun.metadata().into(),
                    tun.forwards_to().into(),
                    tun.proto().to_string(),
                    tun.url().into(),
                );
                self.set_native(jforwarder, tun);
                Ok(jforwarder)
            }
            Err(err) => ngrok_exc_err(err),
        }
    }

    fn listen_http(
        &self,
        this: ComNgrokNativeSession<'local>,
        jttb: ComNgrokHttpBuilder<'local>,
    ) -> Result<com_ngrok::ComNgrokNativeHttpListener<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let bldr = self.http_builder(sess, jttb)?;

        let tun = rt.block_on(bldr.listen());
        match tun {
            Ok(tun) => {
                let jlistener = ComNgrokNativeHttpListener::new_1com_ngrok_native_http_listener(
                    self.env,
                    tun.id().into(),
                    tun.metadata().into(),
                    tun.forwards_to().into(),
                    tun.proto().to_string(),
                    tun.url().into(),
                );
                self.set_native(jlistener, tun);
                Ok(jlistener)
            }
            Err(err) => ngrok_exc_err(err),
        }
    }

    fn forward_http(
        &self,
        this: ComNgrokNativeSession<'local>,
        jttb: ComNgrokHttpBuilder<'local>,
        jurl: com_ngrok::JavaNetUrl<'local>,
    ) -> Result<com_ngrok::ComNgrokNativeHttpForwarder<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let bldr = self.http_builder(sess, jttb)?;

        let url = Url::parse(jurl.as_string(self.env).as_str()).map_err(io_exc)?;

        let tun = rt.block_on(bldr.listen_and_forward(url));
        match tun {
            Ok(tun) => {
                let jforwarder = ComNgrokNativeHttpForwarder::new_1com_ngrok_native_http_forwarder(
                    self.env,
                    tun.id().into(),
                    tun.metadata().into(),
                    tun.forwards_to().into(),
                    tun.proto().to_string(),
                    tun.url().into(),
                );
                self.set_native(jforwarder, tun);
                Ok(jforwarder)
            }
            Err(err) => ngrok_exc_err(err),
        }
    }

    fn listen_edge(
        &self,
        this: ComNgrokNativeSession<'local>,
        jttb: ComNgrokEdgeBuilder<'local>,
    ) -> Result<com_ngrok::ComNgrokNativeEdgeListener<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let bldr = self.edge_builder(sess, jttb)?;

        let tun = rt.block_on(bldr.listen());
        match tun {
            Ok(tun) => {
                let jlistener = ComNgrokNativeEdgeListener::new_1com_ngrok_native_edge_listener(
                    self.env,
                    tun.id().into(),
                    tun.metadata().into(),
                    tun.forwards_to().into(),
                    self.labels_map(tun.labels())
                        .expect("cannot get result labels"),
                );
                self.set_native(jlistener, tun);
                Ok(jlistener)
            }
            Err(err) => ngrok_exc_err(err),
        }
    }

    fn forward_edge(
        &self,
        this: ComNgrokNativeSession<'local>,
        jttb: ComNgrokEdgeBuilder<'local>,
        jurl: com_ngrok::JavaNetUrl<'local>,
    ) -> Result<com_ngrok::ComNgrokNativeEdgeForwarder<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let bldr = self.edge_builder(sess, jttb)?;

        let url = Url::parse(jurl.as_string(self.env).as_str()).map_err(io_exc)?;

        let tun = rt.block_on(bldr.listen_and_forward(url));
        match tun {
            Ok(tun) => {
                let jforwarder = ComNgrokNativeEdgeForwarder::new_1com_ngrok_native_edge_forwarder(
                    self.env,
                    tun.id().into(),
                    tun.metadata().into(),
                    tun.forwards_to().into(),
                    self.labels_map(tun.labels())
                        .expect("cannot get result labels"),
                );
                self.set_native(jforwarder, tun);
                Ok(jforwarder)
            }
            Err(err) => ngrok_exc_err(err),
        }
    }

    fn close_listener(
        &self,
        this: ComNgrokNativeSession<'local>,
        id: String,
    ) -> Result<(), Error<IOExceptionErr>> {
        self.close_tunnel(this, id)
    }

    fn close_forwarder(
        &self,
        this: ComNgrokNativeSession<'local>,
        id: String,
    ) -> Result<(), Error<IOExceptionErr>> {
        self.close_tunnel(this, id)
    }

    fn close(&self, this: ComNgrokNativeSession<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut sess: Session = self.take_native(this);
        rt.block_on(sess.close()).map_err(ngrok_exc)
    }
}

struct NativeTcpListenerRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeTcpListenerRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeTcpListenerRs<'local> for NativeTcpListenerRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn accept(
        &self,
        this: ComNgrokNativeTcpListener<'local>,
    ) -> Result<ComNgrokNativeEndpointConnection<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<TcpTunnel> = self.get_native(this);
        match rt.block_on(tun.try_next()) {
            Ok(Some(conn)) => {
                let jconn: ComNgrokNativeEndpointConnection<'_> =
                    ComNgrokNativeEndpointConnection::new_1com_ngrok_native_endpoint_connection(
                        self.env,
                        conn.remote_addr().to_string(),
                        conn.proto().to_string(),
                    );
                self.set_native(jconn, conn);
                Ok(jconn)
            }
            Ok(None) => io_exc_err("could not get next conn"),
            Err(err) => accept_exc_err(err),
        }
    }

    fn close(&self, this: ComNgrokNativeTcpListener<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: TcpTunnel = self.take_native(this);
        rt.block_on(tun.close()).map_err(ngrok_exc)
    }
}

struct NativeTcpForwarderRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeTcpForwarderRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeTcpForwarderRs<'local> for NativeTcpForwarderRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn join(&self, this: ComNgrokNativeTcpForwarder<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<Forwarder<TcpTunnel>> = self.get_native(this);
        match rt.block_on(tun.join()) {
            Ok(Ok(())) => Ok(()),
            Ok(Err(e)) => io_exc_err(e),
            Err(e) => io_exc_err(e),
        }
    }

    fn close(&self, this: ComNgrokNativeTcpForwarder<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: Forwarder<TcpTunnel> = self.take_native(this);
        rt.block_on(tun.close()).map_err(ngrok_exc)
    }
}

struct NativeTlsListenerRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeTlsListenerRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeTlsListenerRs<'local> for NativeTlsListenerRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn accept(
        &self,
        this: ComNgrokNativeTlsListener<'local>,
    ) -> Result<ComNgrokNativeEndpointConnection<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<TlsTunnel> = self.get_native(this);
        match rt.block_on(tun.try_next()) {
            Ok(Some(conn)) => {
                let jconn =
                    ComNgrokNativeEndpointConnection::new_1com_ngrok_native_endpoint_connection(
                        self.env,
                        conn.remote_addr().to_string(),
                        conn.proto().to_string(),
                    );
                self.set_native(jconn, conn);
                Ok(jconn)
            }
            Ok(None) => io_exc_err("could not get next conn"),
            Err(err) => accept_exc_err(err),
        }
    }

    fn close(&self, this: ComNgrokNativeTlsListener<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: TlsTunnel = self.take_native(this);
        rt.block_on(tun.close()).map_err(ngrok_exc)
    }
}

struct NativeTlsForwarderRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeTlsForwarderRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeTlsForwarderRs<'local> for NativeTlsForwarderRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn join(&self, this: ComNgrokNativeTlsForwarder<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<Forwarder<TlsTunnel>> = self.get_native(this);
        match rt.block_on(tun.join()) {
            Ok(Ok(())) => Ok(()),
            Ok(Err(e)) => io_exc_err(e),
            Err(e) => io_exc_err(e),
        }
    }

    fn close(&self, this: ComNgrokNativeTlsForwarder<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: TlsTunnel = self.take_native(this);
        rt.block_on(tun.close()).map_err(ngrok_exc)
    }
}

struct NativeHttpListenerRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeHttpListenerRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeHttpListenerRs<'local> for NativeHttpListenerRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn accept(
        &self,
        this: ComNgrokNativeHttpListener<'local>,
    ) -> Result<ComNgrokNativeEndpointConnection<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<HttpTunnel> = self.get_native(this);
        match rt.block_on(tun.try_next()) {
            Ok(Some(conn)) => {
                let jconn =
                    ComNgrokNativeEndpointConnection::new_1com_ngrok_native_endpoint_connection(
                        self.env,
                        conn.remote_addr().to_string(),
                        conn.proto().to_string(),
                    );
                self.set_native(jconn, conn);
                Ok(jconn)
            }
            Ok(None) => io_exc_err("could not get next conn"),
            Err(err) => accept_exc_err(err),
        }
    }

    fn close(
        &self,
        this: ComNgrokNativeHttpListener<'local>,
    ) -> Result<(), jaffi_support::Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: HttpTunnel = self.take_native(this);
        rt.block_on(tun.close()).map_err(ngrok_exc)
    }
}

struct NativeHttpForwarderRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeHttpForwarderRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeHttpForwarderRs<'local> for NativeHttpForwarderRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn join(&self, this: ComNgrokNativeHttpForwarder<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<Forwarder<HttpTunnel>> = self.get_native(this);
        match rt.block_on(tun.join()) {
            Ok(Ok(())) => Ok(()),
            Ok(Err(e)) => io_exc_err(e),
            Err(e) => io_exc_err(e),
        }
    }

    fn close(
        &self,
        this: ComNgrokNativeHttpForwarder<'local>,
    ) -> Result<(), jaffi_support::Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: HttpTunnel = self.take_native(this);
        rt.block_on(tun.close()).map_err(ngrok_exc)
    }
}

struct NativeEdgeListenerRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeEdgeListenerRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeEdgeListenerRs<'local> for NativeEdgeListenerRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn accept(
        &self,
        this: ComNgrokNativeEdgeListener<'local>,
    ) -> Result<ComNgrokNativeEdgeConnection<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<LabeledTunnel> = self.get_native(this);
        match rt.block_on(tun.try_next()) {
            Ok(Some(conn)) => {
                let jconn = ComNgrokNativeEdgeConnection::new_1com_ngrok_native_edge_connection(
                    self.env,
                    conn.remote_addr().to_string(),
                    match conn.edge_type() {
                        ngrok::prelude::EdgeType::Https => "HTTPS",
                        ngrok::prelude::EdgeType::Tls => "TLS",
                        ngrok::prelude::EdgeType::Tcp => "TCP",
                        ngrok::prelude::EdgeType::Undefined => "",
                    }
                    .to_string(),
                    conn.passthrough_tls(),
                );
                self.set_native(jconn, conn);
                Ok(jconn)
            }
            Ok(None) => io_exc_err("could not get next conn"),
            Err(err) => accept_exc_err(err),
        }
    }

    fn close(
        &self,
        this: ComNgrokNativeEdgeListener<'local>,
    ) -> Result<(), jaffi_support::Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: LabeledTunnel = self.take_native(this);
        rt.block_on(tun.close()).map_err(ngrok_exc)
    }
}

struct NativeEdgeForwarderRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeEdgeForwarderRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeEdgeForwarderRs<'local> for NativeEdgeForwarderRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn join(&self, this: ComNgrokNativeEdgeForwarder<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<Forwarder<LabeledTunnel>> = self.get_native(this);
        match rt.block_on(tun.join()) {
            Ok(Ok(())) => Ok(()),
            Ok(Err(e)) => io_exc_err(e),
            Err(e) => io_exc_err(e),
        }
    }

    fn close(
        &self,
        this: ComNgrokNativeEdgeForwarder<'local>,
    ) -> Result<(), jaffi_support::Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: LabeledTunnel = self.take_native(this);
        rt.block_on(tun.close()).map_err(ngrok_exc)
    }
}

struct NativeEndpointConnectionRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeEndpointConnectionRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeEndpointConnectionRs<'local>
    for NativeEndpointConnectionRsImpl<'local>
{
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn read_native(
        &self,
        this: ComNgrokNativeEndpointConnection<'local>,
        jbuff: JByteBuffer<'local>,
    ) -> Result<i32, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut conn: MutexGuard<EndpointConn> = self.get_native(this);
        let addr = self
            .env
            .get_direct_buffer_address(jbuff)
            .expect("cannot get buff addr");
        match rt.block_on(conn.read(addr)) {
            Ok(0) => Err(io_exc("closed")),
            Ok(sz) => Ok(sz.try_into().expect("size must be i32")),
            Err(err) => io_exc_err(err),
        }
    }

    fn write_native(
        &self,
        this: ComNgrokNativeEndpointConnection<'local>,
        jbuff: JByteBuffer<'local>,
        limit: i32,
    ) -> Result<i32, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut conn: MutexGuard<EndpointConn> = self.get_native(this);
        let addr = self
            .env
            .get_direct_buffer_address(jbuff)
            .expect("cannot get buff addr");
        let act = &addr[..(limit as usize)];
        match rt.block_on(conn.write(act)) {
            Ok(sz) => Ok(sz.try_into().expect("cannot convert to i32")),
            Err(err) => io_exc_err(err),
        }
    }

    fn close(
        &self,
        this: ComNgrokNativeEndpointConnection<'local>,
    ) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut conn: EndpointConn = self.take_native(this);
        rt.block_on(conn.shutdown()).map_err(io_exc)
    }
}

struct NativeEdgeConnectionRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeEdgeConnectionRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeEdgeConnectionRs<'local> for NativeEdgeConnectionRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn read_native(
        &self,
        this: ComNgrokNativeEdgeConnection<'local>,
        jbuff: JByteBuffer<'local>,
    ) -> Result<i32, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut conn: MutexGuard<EdgeConn> = self.get_native(this);
        let addr = self
            .env
            .get_direct_buffer_address(jbuff)
            .expect("cannot get buff addr");
        match rt.block_on(conn.read(addr)) {
            Ok(0) => Err(io_exc("closed")),
            Ok(sz) => Ok(sz.try_into().expect("size must be i32")),
            Err(err) => io_exc_err(err),
        }
    }

    fn write_native(
        &self,
        this: ComNgrokNativeEdgeConnection<'local>,
        jbuff: JByteBuffer<'local>,
        limit: i32,
    ) -> Result<i32, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut conn: MutexGuard<EdgeConn> = self.get_native(this);
        let addr = self
            .env
            .get_direct_buffer_address(jbuff)
            .expect("cannot get buff addr");
        let act = &addr[..(limit as usize)];
        match rt.block_on(conn.write(act)) {
            Ok(sz) => Ok(sz.try_into().expect("cannot convert to i32")),
            Err(err) => io_exc_err(err),
        }
    }

    fn close(
        &self,
        this: ComNgrokNativeEdgeConnection<'local>,
    ) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut conn: EdgeConn = self.take_native(this);
        rt.block_on(conn.shutdown()).map_err(io_exc)
    }
}
