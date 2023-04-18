use async_trait::async_trait;
use bytes::Bytes;
use com_ngrok::{
    ComNgrokHttpTunnelBuilder, ComNgrokLabeledTunnelBuilder, ComNgrokLabeledTunnelLabel,
    ComNgrokNativeConnection, ComNgrokNativeHttpTunnel, ComNgrokNativeLabeledTunnel,
    ComNgrokNativeSession, ComNgrokNativeSessionClass, ComNgrokNativeTcpTunnel,
    ComNgrokNativeTlsTunnel, ComNgrokRuntimeLogger, ComNgrokSessionBuilder,
    ComNgrokSessionRestartCallback, ComNgrokSessionStopCallback, ComNgrokSessionUpdateCallback,
    ComNgrokSessionUserAgent, ComNgrokTcpTunnelBuilder, ComNgrokTlsTunnelBuilder, IOException,
    IOExceptionErr,
};
use futures::TryStreamExt;
use once_cell::sync::OnceCell;
use std::sync::MutexGuard;
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
    config::ProxyProto,
    prelude::{TunnelBuilder, TunnelExt},
    session::{CommandHandler, Restart, Stop, Update},
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

fn io_exc<E: ToString>(e: E) -> Error<IOExceptionErr> {
    Error::new(IOExceptionErr::IOException(IOException), e.to_string())
}

fn io_exc_err<T, E: ToString>(e: E) -> Result<T, Error<IOExceptionErr>> {
    Err(io_exc(e))
}

struct StopCallback {
    cbk: GlobalRef,
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

        {
            let (user_agents, user_agents_size) = self.get_list_field(jsb, "userAgents");
            for i in 0..user_agents_size {
                let user_agent: ComNgrokSessionUserAgent =
                    self.get_list_item(user_agents, i).into();
                bldr = bldr.child_client(user_agent.name(self.env), user_agent.version(self.env));
            }
        }

        if let Some(authtoken) = self.get_string_field(jsb, "authtoken") {
            bldr = bldr.authtoken(authtoken);
        }

        let mut session_metadata: Option<String> = None;
        if let Some(metadata) = self.get_string_field(jsb, "metadata") {
            session_metadata = Some(metadata.clone());
            bldr = bldr.metadata(metadata);
        }

        let stop_obj: ComNgrokSessionStopCallback = self
            .env
            .get_field(jsb, "stopCallback", "Lcom/ngrok/Session$StopCallback;")
            .and_then(|o| o.l())
            .expect("could not get stopCallback")
            .into();
        if !stop_obj.is_null() {
            let cbk = self
                .env
                .new_global_ref(stop_obj)
                .expect("cannot get global reference");
            bldr = bldr.handle_stop_command(StopCallback { cbk });
        }

        let restart_obj: ComNgrokSessionRestartCallback = self
            .env
            .get_field(
                jsb,
                "restartCallback",
                "Lcom/ngrok/Session$RestartCallback;",
            )
            .and_then(|o| o.l())
            .expect("could not get restartCallback")
            .into();
        if !restart_obj.is_null() {
            let cbk = self
                .env
                .new_global_ref(restart_obj)
                .expect("cannot get global reference");
            bldr = bldr.handle_restart_command(RestartCallback { cbk });
        }

        let update_obj: ComNgrokSessionUpdateCallback = self
            .env
            .get_field(jsb, "updateCallback", "Lcom/ngrok/Session$UpdateCallback;")
            .and_then(|o| o.l())
            .expect("could not get updateCallback")
            .into();
        if !update_obj.is_null() {
            let cbk = self
                .env
                .new_global_ref(update_obj)
                .expect("cannot get global reference");
            bldr = bldr.handle_update_command(UpdateCallback { cbk });
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
        jtb: ComNgrokTcpTunnelBuilder<'local>,
    ) -> Result<ComNgrokNativeTcpTunnel<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let mut bldr = sess.tcp_endpoint();

        // from Tunnel.Builder
        if let Some(metadata) = self.get_string_field(jtb, "metadata") {
            bldr = bldr.metadata(metadata);
        }

        // from AgentTunnel.Builder
        {
            let (allow_cidr, allow_cidr_size) = self.get_list_field(jtb, "allowCIDR");
            for i in 0..allow_cidr_size {
                let cidr: JString = self.get_list_item(allow_cidr, i).into();
                if let Some(cidr) = self.as_string(cidr) {
                    bldr = bldr.allow_cidr(cidr);
                }
            }
        }

        {
            let (deny_cidr, deny_cidr_size) = self.get_list_field(jtb, "denyCIDR");
            for i in 0..deny_cidr_size {
                let cidr: JString = self.get_list_item(deny_cidr, i).into();
                if let Some(cidr) = self.as_string(cidr) {
                    bldr = bldr.deny_cidr(cidr);
                }
            }
        }

        if let Some(proxy_proto) = self.get_proxy_proto(jtb) {
            bldr = bldr.proxy_proto(proxy_proto);
        }

        if let Some(forwards_to) = self.get_string_field(jtb, "forwardsTo") {
            bldr = bldr.forwards_to(forwards_to);
        }

        // from TcpTunnel.Builder
        if let Some(remote_address) = self.get_string_field(jtb, "remoteAddress") {
            bldr = bldr.remote_addr(remote_address);
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
        jtb: ComNgrokTlsTunnelBuilder<'local>,
    ) -> Result<ComNgrokNativeTlsTunnel<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let mut bldr = sess.tls_endpoint();

        // from Tunnel.Builder
        if let Some(metadata) = self.get_string_field(jtb, "metadata") {
            bldr = bldr.metadata(metadata);
        }

        // from AgentTunnel.Builder
        {
            let (allow_cidr, allow_cidr_size) = self.get_list_field(jtb, "allowCIDR");
            for i in 0..allow_cidr_size {
                let cidr: JString = self.get_list_item(allow_cidr, i).into();
                if let Some(cidr) = self.as_string(cidr) {
                    bldr = bldr.allow_cidr(cidr);
                }
            }
        }

        {
            let (deny_cidr, deny_cidr_size) = self.get_list_field(jtb, "denyCIDR");
            for i in 0..deny_cidr_size {
                let cidr: JString = self.get_list_item(deny_cidr, i).into();
                if let Some(cidr) = self.as_string(cidr) {
                    bldr = bldr.deny_cidr(cidr);
                }
            }
        }

        if let Some(proxy_proto) = self.get_proxy_proto(jtb) {
            bldr = bldr.proxy_proto(proxy_proto);
        }

        if let Some(forwards_to) = self.get_string_field(jtb, "forwardsTo") {
            bldr = bldr.forwards_to(forwards_to);
        }

        // from TlsTunnel.Builder
        if let Some(domain) = self.get_string_field(jtb, "domain") {
            bldr = bldr.domain(domain);
        }

        let mtls = jtb.mutual_tlsca(self.env);
        if !mtls.is_null() {
            let mtls_data = mtls.as_slice(&self.env).expect("cannot get mtls data");
            bldr = bldr.mutual_tlsca(Bytes::copy_from_slice(&mtls_data));
        }

        match (
            jtb.termination_cert_pem(self.env),
            jtb.termination_key_pem(self.env),
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
        jtb: ComNgrokHttpTunnelBuilder<'local>,
    ) -> Result<ComNgrokNativeHttpTunnel<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let mut bldr = sess.http_endpoint();

        // from Tunnel.Builder
        if let Some(metadata) = self.get_string_field(jtb, "metadata") {
            bldr = bldr.metadata(metadata);
        }

        // from AgentTunnel.Builder
        {
            let (allow_cidr, allow_cidr_size) = self.get_list_field(jtb, "allowCIDR");
            for i in 0..allow_cidr_size {
                let cidr: JString = self.get_list_item(allow_cidr, i).into();
                if let Some(cidr) = self.as_string(cidr) {
                    bldr = bldr.allow_cidr(cidr);
                }
            }
        }

        {
            let (deny_cidr, deny_cidr_size) = self.get_list_field(jtb, "denyCIDR");
            for i in 0..deny_cidr_size {
                let cidr: JString = self.get_list_item(deny_cidr, i).into();
                if let Some(cidr) = self.as_string(cidr) {
                    bldr = bldr.deny_cidr(cidr);
                }
            }
        }

        if let Some(proxy_proto) = self.get_proxy_proto(jtb) {
            bldr = bldr.proxy_proto(proxy_proto);
        }

        if let Some(forwards_to) = self.get_string_field(jtb, "forwardsTo") {
            bldr = bldr.forwards_to(forwards_to);
        }

        // from HttpTunnel.Builder
        if let Some(domain) = self.get_string_field(jtb, "domain") {
            bldr = bldr.domain(domain);
        }

        let mtls = jtb.mutual_tlsca(self.env);
        if !mtls.is_null() {
            let slice = mtls.as_slice(&self.env).expect("cannot get mtls data");
            bldr = bldr.mutual_tlsca(Bytes::copy_from_slice(&slice));
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
        jtb: ComNgrokLabeledTunnelBuilder<'local>,
    ) -> Result<ComNgrokNativeLabeledTunnel<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: MutexGuard<Session> = self.get_native(this);
        let mut bldr = sess.labeled_tunnel();

        // from Tunnel.Builder
        if let Some(metadata) = self.get_string_field(jtb, "metadata") {
            bldr = bldr.metadata(metadata);
        }

        // from LabeledTunnel.Builder
        {
            let (labels, labels_size) = self.get_list_method(jtb, "labels");
            for i in 0..labels_size {
                let label: ComNgrokLabeledTunnelLabel = self.get_list_item(labels, i).into();
                bldr = bldr.label(label.name(self.env), label.value(self.env));
            }
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
