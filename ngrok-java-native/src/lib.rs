use async_trait::async_trait;
use bytes::Bytes;
use com_ngrok::{
    ComNgrokHttpTunnelBuilder, ComNgrokHttpTunnelHeader, ComNgrokLabeledTunnelBuilder,
    ComNgrokLabeledTunnelLabel, ComNgrokNativeEdgeConnection, ComNgrokNativeEndpointConnection, ComNgrokNativeHttpTunnel,
    ComNgrokNativeLabeledTunnel, ComNgrokNativeSession, ComNgrokNativeSessionClass,
    ComNgrokNativeTcpTunnel, ComNgrokNativeTlsTunnel, ComNgrokRuntimeLogger,
    ComNgrokSessionBuilder, ComNgrokSessionClientInfo, ComNgrokSessionHeartbeatHandler,
    ComNgrokSessionRestartCallback, ComNgrokSessionStopCallback, ComNgrokSessionUpdateCallback,
    ComNgrokTcpTunnelBuilder, ComNgrokTlsTunnelBuilder, IOException, IOExceptionErr, JavaUtilList,
};
use futures::TryStreamExt;
use once_cell::sync::OnceCell;
use std::{str::FromStr, sync::MutexGuard, time::Duration};
use tokio::{io::AsyncReadExt, io::AsyncWriteExt, runtime::Runtime};
use tracing::{level_filters::LevelFilter, Level};
use tracing_subscriber::{prelude::__tracing_subscriber_SubscriberExt, util::SubscriberInitExt};

use jaffi_support::{
    jni::{
        objects::{GlobalRef, JByteBuffer, JObject, JString, JValue},
        JNIEnv, JavaVM,
    },
    Error,
};

use ngrok::{
    config::{OauthOptions, OidcOptions, ProxyProto, Scheme},
    prelude::{TunnelBuilder, EdgeConnInfo, EndpointConnInfo},
    session::{CommandHandler, HeartbeatHandler, Restart, Stop, Update},
    tunnel::{HttpTunnel, LabeledTunnel, TcpTunnel, TlsTunnel, TunnelInfo, EndpointInfo, TunnelCloser},
    conn::ConnInfo,
    Session, EndpointConn, EdgeConn,
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
}

fn io_exc<E: ToString>(e: E) -> Error<IOExceptionErr> {
    Error::new(IOExceptionErr::IOException(IOException), e.to_string())
}

fn io_exc_err<T, E: ToString>(e: E) -> Result<T, Error<IOExceptionErr>> {
    Err(io_exc(e))
}

struct StopCallback {
    cbk: GlobalRef,
}

impl StopCallback {
    fn from(env: JNIEnv<'_>, obj: ComNgrokSessionStopCallback) -> Self {
        StopCallback {
            cbk: env
                .new_global_ref(obj)
                .expect("cannot get global reference"),
        }
    }
}

#[async_trait]
impl CommandHandler<Stop> for StopCallback {
    async fn handle_command(&self, _req: Stop) -> Result<(), String> {
        let jvm = JVM.get().expect("no jvm");
        let jenv = jvm.attach_current_thread().expect("cannot attach");

        let lcbk = ComNgrokSessionStopCallback::from(self.cbk.as_obj());
        lcbk.on_stop_command(*jenv);
        Ok(())
    }
}

struct RestartCallback {
    cbk: GlobalRef,
}

impl RestartCallback {
    fn from(env: JNIEnv<'_>, obj: ComNgrokSessionRestartCallback) -> Self {
        RestartCallback {
            cbk: env
                .new_global_ref(obj)
                .expect("cannot get global reference"),
        }
    }
}

#[async_trait]
impl CommandHandler<Restart> for RestartCallback {
    async fn handle_command(&self, _req: Restart) -> Result<(), String> {
        let jvm = JVM.get().expect("no jvm");
        let jenv = jvm.attach_current_thread().expect("cannot attach");

        let lcbk = ComNgrokSessionRestartCallback::from(self.cbk.as_obj());
        lcbk.on_restart_command(*jenv);
        Ok(())
    }
}

struct UpdateCallback {
    cbk: GlobalRef,
}

impl UpdateCallback {
    fn from(env: JNIEnv<'_>, obj: ComNgrokSessionUpdateCallback) -> Self {
        UpdateCallback {
            cbk: env
                .new_global_ref(obj)
                .expect("cannot get global reference"),
        }
    }
}

#[async_trait]
impl CommandHandler<Update> for UpdateCallback {
    async fn handle_command(&self, _req: Update) -> Result<(), String> {
        let jvm = JVM.get().expect("no jvm");
        let jenv = jvm.attach_current_thread().expect("cannot attach");

        let lcbk = ComNgrokSessionUpdateCallback::from(self.cbk.as_obj());
        lcbk.on_update_command(*jenv);
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

        if jsb.has_authtoken(self.env) {
            bldr.authtoken(jsb.get_authtoken(self.env));
        }

        if jsb.has_heartbeat_interval(self.env) {
            let d: u64 = jsb.get_heartbeat_interval_ms(self.env).try_into().unwrap();
            bldr.heartbeat_interval(Duration::from_millis(d));
        }

        if jsb.has_heartbeat_tolerance(self.env) {
            let d: u64 = jsb.get_heartbeat_tolerance_ms(self.env).try_into().unwrap();
            bldr.heartbeat_tolerance(Duration::from_millis(d));
        }

        let mut session_metadata = String::from("");
        if jsb.has_metadata(self.env) {
            session_metadata = jsb.get_metadata(self.env);
            bldr.metadata(session_metadata.clone());
        }

        if jsb.has_server_addr(self.env) {
            bldr.server_addr(jsb.get_server_addr(self.env));
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
            bldr.handle_stop_command(StopCallback::from(self.env, stop_obj));
        }

        let restart_obj = jsb.restart_callback(self.env);
        if !restart_obj.is_null() {
            bldr.handle_restart_command(RestartCallback::from(self.env, restart_obj));
        }

        let update_obj = jsb.update_callback(self.env);
        if !update_obj.is_null() {
            bldr.handle_update_command(UpdateCallback::from(self.env, update_obj));
        }

        let heartbeat_obj = jsb.heartbeat_handler(self.env);
        if !heartbeat_obj.is_null() {
            bldr.handle_heartbeat(HeartbeatCallback::from(self.env, heartbeat_obj));
        }

        let client_infos = jsb.get_client_infos(self.env);
        for i in 0..client_infos.size(self.env) {
            let client_info: ComNgrokSessionClientInfo = client_infos.get(self.env, i).into();
            let comments = if client_info.has_comments(self.env) {
                Option::<String>::Some(client_info.get_comments(self.env))
            } else {
                Option::<String>::None
            };
            bldr.client_info(
                client_info.get_type(self.env),
                client_info.get_version(self.env),
                comments,
            );
        }

        match rt.block_on(bldr.connect()) {
            Ok(sess) => {
                let jsess = ComNgrokNativeSession::new_1com_ngrok_native_session(
                    self.env,
                    session_metadata,
                );
                self.set_native(jsess, sess);
                Ok(jsess)
            }
            Err(err) => io_exc_err(err),
        }
    }

    fn tcp_tunnel(
        &self,
        this: ComNgrokNativeSession<'local>,
        jttb: ComNgrokTcpTunnelBuilder<'local>,
    ) -> Result<ComNgrokNativeTcpTunnel<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let mut bldr = sess.tcp_endpoint();

        let jatb = jttb.as_com_ngrok_endpoint_tunnel_builder();
        let jtb = jatb.as_com_ngrok_tunnel_builder();

        // from Tunnel.Builder
        if jtb.has_metadata(self.env) {
            bldr.metadata(jtb.get_metadata(self.env));
        }

        // from EndpointTunnel.Builder
        let allow_cidr = jatb.get_allow_cidr(self.env);
        for i in 0..allow_cidr.size(self.env) {
            let cidr: JString = allow_cidr.get(self.env, i).into();
            if let Some(cidr) = self.as_string(cidr) {
                bldr.allow_cidr(cidr);
            }
        }

        let deny_cidr = jatb.get_deny_cidr(self.env);
        for i in 0..deny_cidr.size(self.env) {
            let cidr: JString = deny_cidr.get(self.env, i).into();
            if let Some(cidr) = self.as_string(cidr) {
                bldr.deny_cidr(cidr);
            }
        }

        if jatb.has_proxy_proto(self.env) {
            bldr.proxy_proto(ProxyProto::from(jatb.get_proxy_proto_version(self.env)));
        }

        if jatb.has_forwards_to(self.env) {
            bldr.forwards_to(jatb.get_forwards_to(self.env));
        }

        // from TcpTunnel.Builder
        if jttb.has_remote_address(self.env) {
            bldr.remote_addr(jttb.get_remote_address(self.env));
        }

        let tun = rt.block_on(bldr.listen());
        match tun {
            Ok(tun) => {
                let jtunnel = ComNgrokNativeTcpTunnel::new_1com_ngrok_native_tcp_tunnel(
                    self.env,
                    tun.id().into(),
                    tun.forwards_to().into(),
                    tun.metadata().into(),
                    "tcp".into(),
                    tun.url().into(),
                );
                self.set_native(jtunnel, tun);
                Ok(jtunnel)
            }
            Err(err) => io_exc_err(err),
        }
    }

    fn tls_tunnel(
        &self,
        this: ComNgrokNativeSession<'local>,
        jttb: ComNgrokTlsTunnelBuilder<'local>,
    ) -> Result<ComNgrokNativeTlsTunnel<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let mut bldr = sess.tls_endpoint();

        let jatb = jttb.as_com_ngrok_endpoint_tunnel_builder();
        let jtb = jatb.as_com_ngrok_tunnel_builder();

        // from Tunnel.Builder
        if jtb.has_metadata(self.env) {
            bldr.metadata(jtb.get_metadata(self.env));
        }

        // from EndpointTunnel.Builder
        let allow_cidr = jatb.get_allow_cidr(self.env);
        for i in 0..allow_cidr.size(self.env) {
            let cidr: JString = allow_cidr.get(self.env, i).into();
            if let Some(cidr) = self.as_string(cidr) {
                bldr.allow_cidr(cidr);
            }
        }

        let deny_cidr = jatb.get_deny_cidr(self.env);
        for i in 0..deny_cidr.size(self.env) {
            let cidr: JString = deny_cidr.get(self.env, i).into();
            if let Some(cidr) = self.as_string(cidr) {
                bldr.deny_cidr(cidr);
            }
        }

        if jatb.has_proxy_proto(self.env) {
            bldr.proxy_proto(ProxyProto::from(jatb.get_proxy_proto_version(self.env)));
        }

        if jatb.has_forwards_to(self.env) {
            bldr.forwards_to(jatb.get_forwards_to(self.env));
        }

        // from TlsTunnel.Builder
        if jttb.has_domain(self.env) {
            bldr.domain(jttb.get_domain(self.env));
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

        match rt.block_on(bldr.listen()) {
            Ok(tun) => {
                let jtunnel = ComNgrokNativeTlsTunnel::new_1com_ngrok_native_tls_tunnel(
                    self.env,
                    tun.id().into(),
                    tun.forwards_to().into(),
                    tun.metadata().into(),
                    "tls".into(),
                    tun.url().into(),
                );
                self.set_native(jtunnel, tun);
                Ok(jtunnel)
            }
            Err(err) => io_exc_err(err),
        }
    }

    fn http_tunnel(
        &self,
        this: ComNgrokNativeSession<'local>,
        jhtb: ComNgrokHttpTunnelBuilder<'local>,
    ) -> Result<ComNgrokNativeHttpTunnel<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let mut bldr = sess.http_endpoint();

        let jatb = jhtb.as_com_ngrok_endpoint_tunnel_builder();
        let jtb = jatb.as_com_ngrok_tunnel_builder();

        // from Tunnel.Builder
        if jtb.has_metadata(self.env) {
            bldr.metadata(jtb.get_metadata(self.env));
        }

        // from EndpointTunnel.Builder
        let allow_cidr = jatb.get_allow_cidr(self.env);
        for i in 0..allow_cidr.size(self.env) {
            let cidr: JString = allow_cidr.get(self.env, i).into();
            if let Some(cidr) = self.as_string(cidr) {
                bldr.allow_cidr(cidr);
            }
        }

        let deny_cidr = jatb.get_deny_cidr(self.env);
        for i in 0..deny_cidr.size(self.env) {
            let cidr: JString = deny_cidr.get(self.env, i).into();
            if let Some(cidr) = self.as_string(cidr) {
                bldr.deny_cidr(cidr);
            }
        }

        if jatb.has_proxy_proto(self.env) {
            bldr.proxy_proto(ProxyProto::from(jatb.get_proxy_proto_version(self.env)));
        }

        if jatb.has_forwards_to(self.env) {
            bldr.forwards_to(jatb.get_forwards_to(self.env));
        }

        // from HttpTunnel.Builder
        if jhtb.has_scheme(self.env) {
            let scheme = Scheme::from_str(jhtb.get_scheme_name(self.env).as_str())
                .expect("invalid scheme name");
            bldr.scheme(scheme);
        }

        if jhtb.has_domain(self.env) {
            bldr.domain(jhtb.get_domain(self.env));
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

        if jhtb.has_circuit_breaker(self.env) {
            bldr.circuit_breaker(jhtb.get_circuit_breaker(self.env));
        }

        let request_headers = jhtb.get_request_headers(self.env);
        for i in 0..request_headers.size(self.env) {
            let header: ComNgrokHttpTunnelHeader = request_headers.get(self.env, i).into();
            bldr.request_header(header.get_name(self.env), header.get_value(self.env));
        }

        let response_headers = jhtb.get_response_headers(self.env);
        for i in 0..response_headers.size(self.env) {
            let header: ComNgrokHttpTunnelHeader = response_headers.get(self.env, i).into();
            bldr.response_header(header.get_name(self.env), header.get_value(self.env));
        }

        let remove_request_headers = jhtb.get_remove_request_headers(self.env);
        for i in 0..remove_request_headers.size(self.env) {
            let header: JString = remove_request_headers.get(self.env, i).into();
            if let Some(name) = self.as_string(header) {
                bldr.remove_request_header(name);
            }
        }

        let remove_response_headers = jhtb.get_remove_response_headers(self.env);
        for i in 0..remove_response_headers.size(self.env) {
            let header: JString = remove_response_headers.get(self.env, i).into();
            if let Some(name) = self.as_string(header) {
                bldr.remove_response_header(name);
            }
        }

        let basic_auth = jhtb.get_basic_auth_options(self.env);
        if !basic_auth.is_null() {
            bldr.basic_auth(
                basic_auth.get_username(self.env),
                basic_auth.get_password(self.env),
            );
        }

        let joauth = jhtb.get_oauth_options(self.env);
        if !joauth.is_null() {
            let mut oauth = OauthOptions::new(joauth.get_provider(self.env));
            if joauth.has_client_id(self.env) {
                oauth.client_id(joauth.get_client_id(self.env));
                oauth.client_secret(joauth.get_client_secret(self.env));
            }
            if joauth.has_allow_email(self.env) {
                oauth.allow_email(joauth.get_allow_email(self.env));
            }
            if joauth.has_allow_domain(self.env) {
                oauth.allow_domain(joauth.get_allow_domain(self.env));
            }
            if joauth.has_scope(self.env) {
                oauth.scope(joauth.get_scope(self.env));
            }
            bldr.oauth(oauth);
        }

        let joidc = jhtb.get_oidc_options(self.env);
        if !joidc.is_null() {
            let mut oidc = OidcOptions::new(
                joidc.get_issuer_url(self.env),
                joidc.get_client_id(self.env),
                joidc.get_client_secret(self.env),
            );
            if joidc.has_allow_email(self.env) {
                oidc.allow_email(joidc.get_allow_email(self.env));
            }
            if joidc.has_allow_domain(self.env) {
                oidc.allow_domain(joidc.get_allow_domain(self.env));
            }
            if joidc.has_scope(self.env) {
                oidc.scope(joidc.get_scope(self.env));
            }
            bldr.oidc(oidc);
        }

        let jwv = jhtb.get_webhook_verification(self.env);
        if !jwv.is_null() {
            bldr.webhook_verification(jwv.get_provider(self.env), jwv.get_secret(self.env));
        }

        match rt.block_on(bldr.listen()) {
            Ok(tun) => {
                let jtunnel = ComNgrokNativeHttpTunnel::new_1com_ngrok_native_http_tunnel(
                    self.env,
                    tun.id().into(),
                    tun.forwards_to().into(),
                    tun.metadata().into(),
                    "http".into(),
                    tun.url().into(),
                );
                self.set_native(jtunnel, tun);
                Ok(jtunnel)
            }
            Err(err) => io_exc_err(err),
        }
    }

    fn labeled_tunnel(
        &self,
        this: ComNgrokNativeSession<'local>,
        jltb: ComNgrokLabeledTunnelBuilder<'local>,
    ) -> Result<ComNgrokNativeLabeledTunnel<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let mut bldr = sess.labeled_tunnel();

        let jtb = jltb.as_com_ngrok_tunnel_builder();

        // from Tunnel.Builder
        if jtb.has_metadata(self.env) {
            bldr.metadata(jtb.get_metadata(self.env));
        }

        // from LabeledTunnel.Builder
        let labels = jltb.get_labels(self.env);
        for i in 0..labels.size(self.env) {
            let label: ComNgrokLabeledTunnelLabel = labels.get(self.env, i).into();
            bldr.label(label.get_name(self.env), label.get_value(self.env));
        }

        match rt.block_on(bldr.listen()) {
            Ok(tun) => {
                let jtunnel = ComNgrokNativeLabeledTunnel::new_1com_ngrok_native_labeled_tunnel(
                    self.env,
                    tun.id().into(),
                    tun.forwards_to().into(),
                    tun.metadata().into(),
                );
                self.set_native(jtunnel, tun);
                Ok(jtunnel)
            }
            Err(err) => io_exc_err(err),
        }
    }

    fn close_tunnel(
        &self,
        this: ComNgrokNativeSession<'local>,
        tunnel_id: String,
    ) -> Result<(), jaffi_support::Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        rt.block_on(sess.close_tunnel(tunnel_id)).map_err(io_exc)
    }

    fn close(
        &self,
        this: ComNgrokNativeSession<'local>,
    ) -> Result<(), jaffi_support::Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut sess: Session = self.take_native(this);
        rt.block_on(sess.close()).map_err(io_exc)
    }
}

struct NativeTcpTunnelRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeTcpTunnelRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeTcpTunnelRs<'local> for NativeTcpTunnelRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn accept(
        &self,
        this: ComNgrokNativeTcpTunnel<'local>,
    ) -> Result<ComNgrokNativeEndpointConnection<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<TcpTunnel> = self.get_native(this);
        match rt.block_on(tun.try_next()) {
            Ok(Some(conn)) => {
                let jconn: ComNgrokNativeEndpointConnection<'_> = ComNgrokNativeEndpointConnection::new_1com_ngrok_native_endpoint_connection(
                    self.env,
                    conn.remote_addr().to_string(),
                    conn.proto().to_string(),
                );
                self.set_native(jconn, conn);
                Ok(jconn)
            }
            Ok(None) => io_exc_err("could not get next conn"),
            Err(err) => io_exc_err(err),
        }
    }

    fn forward_tcp(
        &self,
        this: ComNgrokNativeTcpTunnel<'local>,
        addr: String,
    ) -> Result<(), Error<IOExceptionErr>> {
        panic!("not implemented")
        // let rt = RT.get().expect("runtime not initialized");

        // let mut tun: MutexGuard<TcpTunnel> = self.get_native(this);
        // rt.block_on(tun.forward_to(addr)).map_err(io_exc)
    }

    fn close(&self, this: ComNgrokNativeTcpTunnel<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: TcpTunnel = self.take_native(this);
        rt.block_on(tun.close()).map_err(io_exc)
    }
}

struct NativeTlsTunnelRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeTlsTunnelRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeTlsTunnelRs<'local> for NativeTlsTunnelRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn accept(
        &self,
        this: ComNgrokNativeTlsTunnel<'local>,
    ) -> Result<ComNgrokNativeEndpointConnection<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<TlsTunnel> = self.get_native(this);
        match rt.block_on(tun.try_next()) {
            Ok(Some(conn)) => {
                let jconn = ComNgrokNativeEndpointConnection::new_1com_ngrok_native_endpoint_connection(
                    self.env,
                    conn.remote_addr().to_string(),
                    conn.proto().to_string(),
                );
                self.set_native(jconn, conn);
                Ok(jconn)
            }
            Ok(None) => io_exc_err("could not get next conn"),
            Err(err) => io_exc_err(err),
        }
    }

    fn forward_tcp(
        &self,
        this: ComNgrokNativeTlsTunnel<'local>,
        addr: String,
    ) -> Result<(), Error<IOExceptionErr>> {
        panic!("not implemented")
        // let rt = RT.get().expect("runtime not initialized");

        // let mut tun: MutexGuard<TlsTunnel> = self.get_native(this);
        // rt.block_on(tun.forward_tcp(addr)).map_err(io_exc)
    }

    fn close(&self, this: ComNgrokNativeTlsTunnel<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: TlsTunnel = self.take_native(this);
        rt.block_on(tun.close()).map_err(io_exc)
    }
}

struct NativeHttpTunnelRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeHttpTunnelRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeHttpTunnelRs<'local> for NativeHttpTunnelRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn accept(
        &self,
        this: ComNgrokNativeHttpTunnel<'local>,
    ) -> Result<ComNgrokNativeEndpointConnection<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<HttpTunnel> = self.get_native(this);
        match rt.block_on(tun.try_next()) {
            Ok(Some(conn)) => {
                let jconn = ComNgrokNativeEndpointConnection::new_1com_ngrok_native_endpoint_connection(
                    self.env,
                    conn.remote_addr().to_string(),
                    conn.proto().to_string(),
                );
                self.set_native(jconn, conn);
                Ok(jconn)
            }
            Ok(None) => io_exc_err("could not get next conn"),
            Err(err) => io_exc_err(err),
        }
    }

    fn forward_tcp(
        &self,
        this: ComNgrokNativeHttpTunnel<'local>,
        addr: String,
    ) -> Result<(), Error<IOExceptionErr>> {
        panic!("not implemented")
        // let rt = RT.get().expect("runtime not initialized");

        // let mut tun: MutexGuard<HttpTunnel> = self.get_native(this);
        // rt.block_on(tun.forward_tcp(addr)).map_err(io_exc)
    }

    fn close(
        &self,
        this: ComNgrokNativeHttpTunnel<'local>,
    ) -> Result<(), jaffi_support::Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: HttpTunnel = self.take_native(this);
        rt.block_on(tun.close()).map_err(io_exc)
    }
}

struct NativeLabeledTunnelRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeLabeledTunnelRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeLabeledTunnelRs<'local> for NativeLabeledTunnelRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn accept(
        &self,
        this: ComNgrokNativeLabeledTunnel<'local>,
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
                    }.to_string(),
                    conn.passthrough_tls(),
                );
                self.set_native(jconn, conn);
                Ok(jconn)
            }
            Ok(None) => io_exc_err("could not get next conn"),
            Err(err) => io_exc_err(err),
        }
    }

    fn forward_tcp(
        &self,
        this: ComNgrokNativeLabeledTunnel<'local>,
        addr: String,
    ) -> Result<(), Error<IOExceptionErr>> {
        panic!("not implemented")
        // let rt = RT.get().expect("runtime not initialized");

        // let mut tun: MutexGuard<LabeledTunnel> = self.get_native(this);
        // rt.block_on(tun.forward_tcp(addr)).map_err(io_exc)
    }

    fn close(
        &self,
        this: ComNgrokNativeLabeledTunnel<'local>,
    ) -> Result<(), jaffi_support::Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: LabeledTunnel = self.take_native(this);
        rt.block_on(tun.close()).map_err(io_exc)
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

impl<'local> com_ngrok::NativeEndpointConnectionRs<'local> for NativeEndpointConnectionRsImpl<'local> {
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
        let act = &addr[..limit.try_into().expect("xxx")];
        match rt.block_on(conn.write(act)) {
            Ok(sz) => Ok(sz.try_into().expect("cannot convert to i32")),
            Err(err) => io_exc_err(err),
        }
    }

    fn close(&self, this: ComNgrokNativeEndpointConnection<'local>) -> Result<(), Error<IOExceptionErr>> {
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
        let act = &addr[..limit.try_into().expect("xxx")];
        match rt.block_on(conn.write(act)) {
            Ok(sz) => Ok(sz.try_into().expect("cannot convert to i32")),
            Err(err) => io_exc_err(err),
        }
    }

    fn close(&self, this: ComNgrokNativeEdgeConnection<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut conn: EdgeConn = self.take_native(this);
        rt.block_on(conn.shutdown()).map_err(io_exc)
    }
}
