use async_trait::async_trait;
use bytes::Bytes;
use com_ngrok::{
    ComNgrokHttpTunnelBuilder, ComNgrokHttpTunnelHeader, ComNgrokLabeledTunnelBuilder,
    ComNgrokLabeledTunnelLabel, ComNgrokNativeConnection, ComNgrokNativeHttpTunnel,
    ComNgrokNativeLabeledTunnel, ComNgrokNativeSession, ComNgrokNativeSessionClass,
    ComNgrokNativeTcpTunnel, ComNgrokNativeTlsTunnel, ComNgrokRuntimeLogger,
    ComNgrokSessionBuilder, ComNgrokSessionHeartbeatHandler, ComNgrokSessionRestartCallback,
    ComNgrokSessionStopCallback, ComNgrokSessionUpdateCallback, ComNgrokSessionUserAgent,
    ComNgrokTcpTunnelBuilder, ComNgrokTlsTunnelBuilder, IOException, IOExceptionErr, JavaUtilList,
};
use futures::TryStreamExt;
use once_cell::sync::OnceCell;
use std::{sync::MutexGuard, time::Duration};
use tokio::{io::AsyncReadExt, io::AsyncWriteExt, runtime::Runtime};
use tracing::metadata::LevelFilter;
use tracing_subscriber::{prelude::__tracing_subscriber_SubscriberExt, util::SubscriberInitExt};

use jaffi_support::{
    jni::{
        objects::{GlobalRef, JByteBuffer, JObject, JString, JValue},
        JNIEnv, JavaVM,
    },
    Error,
};

use ngrok::{
    config::{OauthOptions, OidcOptions, ProxyProto},
    prelude::{TunnelBuilder, TunnelExt},
    session::{CommandHandler, HeartbeatHandler, Restart, Stop, Update},
    tunnel::{HttpTunnel, LabeledTunnel, TcpTunnel, TlsTunnel, UrlTunnel},
    Conn, Session, Tunnel,
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

                tracing_subscriber::registry()
                    .with(TracingLoggingLayer)
                    .with(LevelFilter::TRACE)
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

    fn set_string_field<O>(&self, obj: O, name: &str, value: &str)
    where
        O: Into<JObject<'local>>,
    {
        let jvalue: JValue = self
            .get_env()
            .new_string(value)
            .expect("could not convert to string")
            .into();
        self.get_env()
            .set_field(obj, name, "Ljava/lang/String;", jvalue)
            .expect("could not set string field")
    }

    fn get_string_field<O>(&self, obj: O, name: &str) -> Option<String>
    where
        O: Into<JObject<'local>>,
    {
        let jstr: JString = self
            .get_env()
            .get_field(obj, name, "Ljava/lang/String;")
            .and_then(|o| o.l())
            .expect("could not get string field")
            .into();

        self.as_string(jstr)
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

    fn get_list_field<'env, O>(&self, obj: O, name: &str) -> (JObject<'env>, i32)
    where
        'local: 'env,
        O: Into<JObject<'local>>,
    {
        let list = self
            .get_env()
            .get_field(obj, name, "Ljava/util/List;")
            .and_then(|o| o.l())
            .expect("could not get list field");
        (list, self.get_list_size(list))
    }

    fn get_list_method<'env, O>(&self, obj: O, name: &str) -> (JObject<'env>, i32)
    where
        'local: 'env,
        O: Into<JObject<'local>>,
    {
        let list = self
            .get_env()
            .call_method(obj, name, "()Ljava/util/List;", &[])
            .and_then(|o| o.l())
            .expect("could not get list method");
        (list, self.get_list_size(list))
    }

    fn get_list_size<O>(&self, list: O) -> i32
    where
        O: Into<JObject<'local>>,
    {
        self.get_env()
            .call_method(list, "size", "()I", &[])
            .and_then(|o| o.i())
            .expect("could not get list size")
    }

    fn get_list_item<'env, O>(&self, list: O, idx: i32) -> JObject<'env>
    where
        'local: 'env,
        O: Into<JObject<'local>>,
    {
        self.get_env()
            .call_method(list, "get", "(I)Ljava/lang/Object;", &[JValue::Int(idx)])
            .and_then(|o| o.l())
            .expect("could not get list item")
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
        lcbk.stop(*jenv);
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
        lcbk.restart(*jenv);
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
        lcbk.update(*jenv);
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
    fn get_proxy_proto<O>(&self, obj: O) -> Option<ProxyProto>
    where
        O: Into<JObject<'local>>,
    {
        let proxy_proto = self
            .env
            .get_field(obj, "proxyProto", "Lcom/ngrok/ProxyProto;")
            .and_then(|o| o.l())
            .expect("could not get proxy proto field");

        if proxy_proto.is_null() {
            return None;
        }

        let version = self
            .env
            .get_field(proxy_proto, "version", "I")
            .and_then(|o| o.i())
            .expect("could not get version field");
        Some(ProxyProto::from(i64::from(version)))
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

        let user_agents = jsb.get_user_agents(self.env);
        for i in 0..user_agents.size(self.env) {
            let user_agent: ComNgrokSessionUserAgent = user_agents.get(self.env, i).into();
            bldr = bldr.child_client(
                user_agent.get_name(self.env),
                user_agent.get_version(self.env),
            );
        }

        if jsb.has_authtoken(self.env) {
            bldr = bldr.authtoken(jsb.get_authtoken(self.env));
        }

        // TODO: heartbeat_interval
        // TODO: heartbeat_tolerance

        let mut session_metadata: Option<String> = None;
        if jsb.has_metadata(self.env) {
            session_metadata = Some(jsb.get_metadata(self.env));
            bldr = bldr.metadata(jsb.get_metadata(self.env));
        }

        // TODO: server_addr
        // TODO: ca_cert
        // TODO: tls_config
        // TODO: connector?

        let stop_obj = jsb.stop_callback(self.env);
        if !stop_obj.is_null() {
            bldr = bldr.handle_stop_command(StopCallback::from(self.env, stop_obj));
        }

        let restart_obj = jsb.restart_callback(self.env);
        if !restart_obj.is_null() {
            bldr = bldr.handle_restart_command(RestartCallback::from(self.env, restart_obj));
        }

        let update_obj = jsb.update_callback(self.env);
        if !update_obj.is_null() {
            bldr = bldr.handle_update_command(UpdateCallback::from(self.env, update_obj));
        }

        let heartbeat_obj = jsb.heartbeat_handler(self.env);
        if !heartbeat_obj.is_null() {
            bldr = bldr.handle_heartbeat(HeartbeatCallback::from(self.env, heartbeat_obj));
        }

        match rt.block_on(bldr.connect()) {
            Ok(sess) => {
                let jsess = ComNgrokNativeSession::new_1com_ngrok_native_session(self.env);

                if let Some(metadata) = session_metadata {
                    self.set_string_field(jsess, "metadata", &metadata);
                }

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

        let jatb = jttb.as_com_ngrok_agent_tunnel_builder();
        let jtb = jatb.as_com_ngrok_tunnel_builder();

        // from Tunnel.Builder
        if jtb.has_metadata(self.env) {
            bldr = bldr.metadata(jtb.get_metadata(self.env));
        }

        // from AgentTunnel.Builder
        let allow_cidr = jatb.get_allow_cidr(self.env);
        for i in 0..allow_cidr.size(self.env) {
            let cidr: JString = allow_cidr.get(self.env, i).into();
            if let Some(cidr) = self.as_string(cidr) {
                bldr = bldr.allow_cidr(cidr);
            }
        }

        let deny_cidr = jatb.get_deny_cidr(self.env);
        for i in 0..deny_cidr.size(self.env) {
            let cidr: JString = deny_cidr.get(self.env, i).into();
            if let Some(cidr) = self.as_string(cidr) {
                bldr = bldr.deny_cidr(cidr);
            }
        }

        if let Some(proxy_proto) = self.get_proxy_proto(jttb) {
            bldr = bldr.proxy_proto(proxy_proto);
        }

        if jatb.has_forwards_to(self.env) {
            bldr = bldr.forwards_to(jatb.get_forwards_to(self.env));
        }

        // from TcpTunnel.Builder
        if jttb.has_remote_address(self.env) {
            bldr = bldr.remote_addr(jttb.get_remote_address(self.env));
        }

        let tun = rt.block_on(bldr.listen());
        match tun {
            Ok(tun) => {
                let jtunnel = ComNgrokNativeTcpTunnel::new_1com_ngrok_native_tcp_tunnel(self.env);

                self.set_string_field(jtunnel, "id", tun.id());
                self.set_string_field(jtunnel, "forwardsTo", tun.forwards_to());
                self.set_string_field(jtunnel, "metadata", tun.metadata());
                self.set_string_field(jtunnel, "proto", "tcp");
                self.set_string_field(jtunnel, "url", tun.url());

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

        let jatb = jttb.as_com_ngrok_agent_tunnel_builder();
        let jtb = jatb.as_com_ngrok_tunnel_builder();

        // from Tunnel.Builder
        if jtb.has_metadata(self.env) {
            bldr = bldr.metadata(jtb.get_metadata(self.env));
        }

        // from AgentTunnel.Builder
        let allow_cidr = jatb.get_allow_cidr(self.env);
        for i in 0..allow_cidr.size(self.env) {
            let cidr: JString = allow_cidr.get(self.env, i).into();
            if let Some(cidr) = self.as_string(cidr) {
                bldr = bldr.allow_cidr(cidr);
            }
        }

        let deny_cidr = jatb.get_deny_cidr(self.env);
        for i in 0..deny_cidr.size(self.env) {
            let cidr: JString = deny_cidr.get(self.env, i).into();
            if let Some(cidr) = self.as_string(cidr) {
                bldr = bldr.deny_cidr(cidr);
            }
        }

        if let Some(proxy_proto) = self.get_proxy_proto(jttb) {
            bldr = bldr.proxy_proto(proxy_proto);
        }

        if jatb.has_forwards_to(self.env) {
            bldr = bldr.forwards_to(jatb.get_forwards_to(self.env));
        }

        // from TlsTunnel.Builder
        if jttb.has_domain(self.env) {
            bldr = bldr.domain(jttb.get_domain(self.env));
        }

        let mtls = jttb.get_mutual_tlsca(self.env);
        if !mtls.is_null() {
            let mtls_data = mtls.as_slice(&self.env).expect("cannot get mtls data");
            bldr = bldr.mutual_tlsca(Bytes::copy_from_slice(&mtls_data));
        }

        match (
            jttb.get_termination_cert_pem(self.env),
            jttb.get_termination_key_pem(self.env),
        ) {
            (cert, key) if !cert.is_null() && !key.is_null() => {
                let cert_pem_data = cert.as_slice(&self.env).expect("cannot get cert data");
                let key_pem_data = key.as_slice(&self.env).expect("cannot get key data");
                bldr = bldr.termination(
                    Bytes::copy_from_slice(&cert_pem_data),
                    Bytes::copy_from_slice(&key_pem_data),
                );
            }
            (cert, key) if cert.is_null() && key.is_null() => {}
            _ => return io_exc_err("requires both terminationCertPEM and terminationKeyPEM"),
        }

        match rt.block_on(bldr.listen()) {
            Ok(tun) => {
                let jtunnel = ComNgrokNativeTlsTunnel::new_1com_ngrok_native_tls_tunnel(self.env);

                self.set_string_field(jtunnel, "id", tun.id());
                self.set_string_field(jtunnel, "forwardsTo", tun.forwards_to());
                self.set_string_field(jtunnel, "metadata", tun.metadata());
                self.set_string_field(jtunnel, "proto", "tcp");
                self.set_string_field(jtunnel, "url", tun.url());

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

        let jatb = jhtb.as_com_ngrok_agent_tunnel_builder();
        let jtb = jatb.as_com_ngrok_tunnel_builder();

        // from Tunnel.Builder
        if jtb.has_metadata(self.env) {
            bldr = bldr.metadata(jtb.get_metadata(self.env));
        }

        // from AgentTunnel.Builder
        let allow_cidr = jatb.get_allow_cidr(self.env);
        for i in 0..allow_cidr.size(self.env) {
            let cidr: JString = allow_cidr.get(self.env, i).into();
            if let Some(cidr) = self.as_string(cidr) {
                bldr = bldr.allow_cidr(cidr);
            }
        }

        let deny_cidr = jatb.get_deny_cidr(self.env);
        for i in 0..deny_cidr.size(self.env) {
            let cidr: JString = deny_cidr.get(self.env, i).into();
            if let Some(cidr) = self.as_string(cidr) {
                bldr = bldr.deny_cidr(cidr);
            }
        }

        if let Some(proxy_proto) = self.get_proxy_proto(jhtb) {
            bldr = bldr.proxy_proto(proxy_proto);
        }

        if jatb.has_forwards_to(self.env) {
            bldr = bldr.forwards_to(jatb.get_forwards_to(self.env));
        }

        // from HttpTunnel.Builder

        // TODO: scheme

        if jhtb.has_domain(self.env) {
            bldr = bldr.domain(jhtb.get_domain(self.env));
        }

        let mtls = jhtb.get_mutual_tlsca(self.env);
        if !mtls.is_null() {
            let slice = mtls.as_slice(&self.env).expect("cannot get mtls data");
            bldr = bldr.mutual_tlsca(Bytes::copy_from_slice(&slice));
        }

        if jhtb.is_compression(self.env) {
            bldr = bldr.compression();
        }

        if jhtb.is_websocket_tcp_conversion(self.env) {
            bldr = bldr.websocket_tcp_conversion();
        }

        if jhtb.has_circuit_breaker(self.env) {
            bldr = bldr.circuit_breaker(jhtb.get_circuit_breaker(self.env));
        }

        let request_headers = jhtb.get_request_headers(self.env);
        for i in 0..request_headers.size(self.env) {
            let header: ComNgrokHttpTunnelHeader = request_headers.get(self.env, i).into();
            bldr = bldr.request_header(header.get_name(self.env), header.get_value(self.env));
        }

        let response_headers = jhtb.get_response_headers(self.env);
        for i in 0..response_headers.size(self.env) {
            let header: ComNgrokHttpTunnelHeader = response_headers.get(self.env, i).into();
            bldr = bldr.response_header(header.get_name(self.env), header.get_value(self.env));
        }

        let remove_request_headers = jhtb.get_remove_request_headers(self.env);
        for i in 0..remove_request_headers.size(self.env) {
            let header: JString = remove_request_headers.get(self.env, i).into();
            if let Some(name) = self.as_string(header) {
                bldr = bldr.remove_request_header(name);
            }
        }

        let remove_response_headers = jhtb.get_remove_response_headers(self.env);
        for i in 0..remove_response_headers.size(self.env) {
            let header: JString = remove_response_headers.get(self.env, i).into();
            if let Some(name) = self.as_string(header) {
                bldr = bldr.remove_response_header(name);
            }
        }

        let basic_auth = jhtb.get_basic_auth_options(self.env);
        if !basic_auth.is_null() {
            bldr = bldr.basic_auth(
                basic_auth.get_username(self.env),
                basic_auth.get_password(self.env),
            );
        }

        let joauth = jhtb.get_oauth_options(self.env);
        if !joauth.is_null() {
            let mut oauth = OauthOptions::new(joauth.get_provider(self.env));
            if joauth.has_client_id(self.env) {
                oauth = oauth.client_id(joauth.get_client_id(self.env));
                oauth = oauth.client_secret(joauth.get_client_secret(self.env));
            }
            if joauth.has_allow_email(self.env) {
                oauth = oauth.allow_email(joauth.get_allow_email(self.env));
            }
            if joauth.has_allow_domain(self.env) {
                oauth = oauth.allow_domain(joauth.get_allow_domain(self.env));
            }
            if joauth.has_scope(self.env) {
                oauth = oauth.scope(joauth.get_scope(self.env));
            }
            bldr = bldr.oauth(oauth);
        }

        let joidc = jhtb.get_oidc_options(self.env);
        if !joidc.is_null() {
            let mut oidc = OidcOptions::new(
                joidc.get_issuer_url(self.env),
                joidc.get_client_id(self.env),
                joidc.get_client_secret(self.env),
            );
            if joidc.has_allow_email(self.env) {
                oidc = oidc.allow_email(joidc.get_allow_email(self.env));
            }
            if joidc.has_allow_domain(self.env) {
                oidc = oidc.allow_domain(joidc.get_allow_domain(self.env));
            }
            if joidc.has_scope(self.env) {
                oidc = oidc.scope(joidc.get_scope(self.env));
            }
            bldr = bldr.oidc(oidc);
        }

        let jwv = jhtb.get_webhook_verification(self.env);
        if !jwv.is_null() {
            bldr = bldr.webhook_verification(jwv.get_provider(self.env), jwv.get_secret(self.env));
        }

        match rt.block_on(bldr.listen()) {
            Ok(tun) => {
                let jtunnel = ComNgrokNativeHttpTunnel::new_1com_ngrok_native_http_tunnel(self.env);

                self.set_string_field(jtunnel, "id", tun.id());
                self.set_string_field(jtunnel, "forwardsTo", tun.forwards_to());
                self.set_string_field(jtunnel, "metadata", tun.metadata());
                self.set_string_field(jtunnel, "proto", "http");
                self.set_string_field(jtunnel, "url", tun.url());

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
            bldr = bldr.metadata(jtb.get_metadata(self.env));
        }

        // from LabeledTunnel.Builder
        let labels = jltb.get_labels(self.env);
        for i in 0..labels.size(self.env) {
            let label: ComNgrokLabeledTunnelLabel = labels.get(self.env, i).into();
            bldr = bldr.label(label.get_name(self.env), label.get_value(self.env));
        }

        match rt.block_on(bldr.listen()) {
            Ok(tun) => {
                let jtunnel =
                    ComNgrokNativeLabeledTunnel::new_1com_ngrok_native_labeled_tunnel(self.env);

                self.set_string_field(jtunnel, "id", tun.id());
                self.set_string_field(jtunnel, "forwardsTo", tun.forwards_to());
                self.set_string_field(jtunnel, "metadata", tun.metadata());

                self.set_native(jtunnel, tun);

                Ok(jtunnel)
            }
            Err(err) => io_exc_err(err),
        }
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
    ) -> Result<ComNgrokNativeConnection<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<TcpTunnel> = self.get_native(this);
        match rt.block_on(tun.try_next()) {
            Ok(Some(conn)) => {
                let jconn = ComNgrokNativeConnection::new_1com_ngrok_native_connection(self.env);

                self.set_string_field(jconn, "remoteAddr", &conn.remote_addr().to_string());

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
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<TcpTunnel> = self.get_native(this);
        rt.block_on(tun.forward_tcp(addr)).map_err(io_exc)
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
    ) -> Result<ComNgrokNativeConnection<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<TlsTunnel> = self.get_native(this);
        match rt.block_on(tun.try_next()) {
            Ok(Some(conn)) => {
                let jconn = ComNgrokNativeConnection::new_1com_ngrok_native_connection(self.env);

                self.set_string_field(jconn, "remoteAddr", &conn.remote_addr().to_string());

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
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<TlsTunnel> = self.get_native(this);
        rt.block_on(tun.forward_tcp(addr)).map_err(io_exc)
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
    ) -> Result<ComNgrokNativeConnection<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<HttpTunnel> = self.get_native(this);
        match rt.block_on(tun.try_next()) {
            Ok(Some(conn)) => {
                let jconn = ComNgrokNativeConnection::new_1com_ngrok_native_connection(self.env);

                self.set_string_field(jconn, "remoteAddr", &conn.remote_addr().to_string());

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
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<HttpTunnel> = self.get_native(this);
        rt.block_on(tun.forward_tcp(addr)).map_err(io_exc)
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
    ) -> Result<ComNgrokNativeConnection<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<LabeledTunnel> = self.get_native(this);
        match rt.block_on(tun.try_next()) {
            Ok(Some(conn)) => {
                let jconn = ComNgrokNativeConnection::new_1com_ngrok_native_connection(self.env);

                self.set_string_field(jconn, "remoteAddr", &conn.remote_addr().to_string());

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
        let rt = RT.get().expect("runtime not initialized");

        let mut tun: MutexGuard<LabeledTunnel> = self.get_native(this);
        rt.block_on(tun.forward_tcp(addr)).map_err(io_exc)
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

struct NativeConnectionRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> JNIExt<'local> for NativeConnectionRsImpl<'local> {
    fn get_env(&self) -> &JNIEnv<'local> {
        &self.env
    }
}

impl<'local> com_ngrok::NativeConnectionRs<'local> for NativeConnectionRsImpl<'local> {
    fn from_env(env: JNIEnv<'local>) -> Self {
        Self { env }
    }

    fn read_native(
        &self,
        this: ComNgrokNativeConnection<'local>,
        jbuff: JByteBuffer<'local>,
    ) -> Result<i32, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut conn: MutexGuard<Conn> = self.get_native(this);
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
        this: ComNgrokNativeConnection<'local>,
        jbuff: JByteBuffer<'local>,
        limit: i32,
    ) -> Result<i32, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut conn: MutexGuard<Conn> = self.get_native(this);
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

    fn close(&self, this: ComNgrokNativeConnection<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut conn: Conn = self.take_native(this);
        rt.block_on(conn.shutdown()).map_err(io_exc)
    }
}
