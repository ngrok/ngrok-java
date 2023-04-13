use async_trait::async_trait;
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

trait SetStringField<'local> {
    fn get_env(&self) -> JNIEnv<'local>;

    fn set_string_field<O>(&self, obj: O, name: &str, value: &str) -> Result<(), anyhow::Error>
    where
        O: Into<JObject<'local>>,
    {
        let jvalue = self.get_env().new_string(value)?;
        self.get_env().set_field(
            obj,
            name,
            "Ljava/lang/String;",
            JValue::Object(jvalue.into()),
        )?;
        Ok(())
    }

    fn get_string_field<'env, O>(&self, obj: O, name: &str) -> Result<Option<String>, anyhow::Error>
    where
        O: Into<JObject<'local>>,
    {
        let jval = self.get_env().get_field(obj, name, "Ljava/lang/String;")?;
        let jstr = jval.l().map(JString::from)?;
        if jstr.is_null() {
            Ok(None)
        } else {
            let s = self.get_env().get_string(jstr)?.into();
            Ok(Some(s))
        }
    }
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

impl<'local> SetStringField<'local> for NativeSessionRsImpl<'local> {
    fn get_env(&self) -> JNIEnv<'local> {
        self.env
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

        let jsess = ComNgrokNativeSession::new_1com_ngrok_native_session(self.env);

        let mut bldr = Session::builder();

        {
            let user_agents = self
                .env
                .get_field(jsb, "userAgents", "Ljava/util/List;")
                .and_then(|o| o.l())
                .expect("could not get versions field");
            let user_agents_count = self
                .env
                .call_method(user_agents, "size", "()I", &[])
                .and_then(|o| o.i())
                .expect("could not get versions size");
            for i in 0..user_agents_count {
                let user_agent: ComNgrokSessionUserAgent = self
                    .env
                    .call_method(
                        user_agents,
                        "get",
                        "(I)Ljava/lang/Object;",
                        &[JValue::Int(i)],
                    )
                    .and_then(|o| o.l())
                    .expect("could not get version object")
                    .into();
                bldr = bldr.child_client(user_agent.name(self.env), user_agent.version(self.env));
            }
        }

        match self.get_string_field(jsb, "authtoken") {
            Ok(Some(authtoken)) => {
                bldr = bldr.authtoken(authtoken);
            }
            Ok(None) => {}
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        let mut session_metadata: Option<String> = None;
        match self.get_string_field(jsb, "metadata") {
            Ok(Some(metadata)) => {
                session_metadata = Some(metadata.clone());
                bldr = bldr.metadata(metadata);
            }
            Ok(None) => {}
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
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

        let sess = rt.block_on(bldr.connect());
        match sess {
            Ok(sess) => {
                if let Some(metadata) = session_metadata {
                    self.set_string_field(jsess, "metadata", &metadata)
                        .expect("cannot set metadata");
                }
                self.env
                    .set_rust_field(jsess, "native_address", sess)
                    .unwrap_or_else(|err| {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    });
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(jsess)
    }

    fn tcp_tunnel(
        &self,
        this: ComNgrokNativeSession<'local>,
        jtb: ComNgrokTcpTunnelBuilder<'local>,
    ) -> Result<ComNgrokNativeTcpTunnel<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let jtunnel = ComNgrokNativeTcpTunnel::new_1com_ngrok_native_tcp_tunnel(self.env);

        let sess: Result<MutexGuard<'_, Session>, _> =
            self.env.get_rust_field(this, "native_address");
        match sess {
            Ok(mu) => {
                let mut bldr = mu.tcp_endpoint();

                match self.get_string_field(jtb, "metadata") {
                    Ok(Some(metadata)) => {
                        bldr = bldr.metadata(metadata);
                    }
                    Ok(None) => {}
                    Err(err) => {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    }
                }

                let tun = rt.block_on(bldr.listen());
                match tun {
                    Ok(tun) => {
                        self.set_string_field(jtunnel, "id", tun.id())
                            .expect("could not set id");
                        self.set_string_field(jtunnel, "forwardsTo", tun.forwards_to())
                            .expect("could not set forwards_to");
                        self.set_string_field(jtunnel, "metadata", tun.metadata())
                            .expect("could not set metadata");
                        self.set_string_field(jtunnel, "proto", "tcp")
                            .expect("could not set proto");
                        self.set_string_field(jtunnel, "url", tun.url())
                            .expect("could not set url");

                        self.env
                            .set_rust_field(jtunnel, "native_address", tun)
                            .unwrap_or_else(|err| {
                                self.env
                                    .throw(err.to_string())
                                    .expect("could not throw exception");
                            })
                    }
                    Err(err) => {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    }
                }
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(jtunnel)
    }

    fn tls_tunnel(
        &self,
        this: ComNgrokNativeSession<'local>,
        jtb: ComNgrokTlsTunnelBuilder<'local>,
    ) -> Result<ComNgrokNativeTlsTunnel<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let jtunnel = ComNgrokNativeTlsTunnel::new_1com_ngrok_native_tls_tunnel(self.env);

        let sess: Result<MutexGuard<'_, Session>, _> =
            self.env.get_rust_field(this, "native_address");
        match sess {
            Ok(mu) => {
                let mut bldr = mu.tls_endpoint();

                match self.get_string_field(jtb, "metadata") {
                    Ok(Some(metadata)) => {
                        bldr = bldr.metadata(metadata);
                    }
                    Ok(None) => {}
                    Err(err) => {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    }
                }

                match self.get_string_field(jtb, "domain") {
                    Ok(Some(domain)) => {
                        bldr = bldr.domain(domain);
                    }
                    Ok(None) => {}
                    Err(err) => {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    }
                }

                let tun = rt.block_on(bldr.listen());
                match tun {
                    Ok(tun) => {
                        self.set_string_field(jtunnel, "id", tun.id())
                            .expect("could not set id");
                        self.set_string_field(jtunnel, "forwardsTo", tun.forwards_to())
                            .expect("could not set forwards_to");
                        self.set_string_field(jtunnel, "metadata", tun.metadata())
                            .expect("could not set metadata");
                        self.set_string_field(jtunnel, "proto", "tcp")
                            .expect("could not set proto");
                        self.set_string_field(jtunnel, "url", tun.url())
                            .expect("could not set url");

                        self.env
                            .set_rust_field(jtunnel, "native_address", tun)
                            .unwrap_or_else(|err| {
                                self.env
                                    .throw(err.to_string())
                                    .expect("could not throw exception");
                            })
                    }
                    Err(err) => {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    }
                }
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(jtunnel)
    }

    fn http_tunnel(
        &self,
        this: ComNgrokNativeSession<'local>,
        jtb: ComNgrokHttpTunnelBuilder<'local>,
    ) -> Result<ComNgrokNativeHttpTunnel<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let jtunnel = ComNgrokNativeHttpTunnel::new_1com_ngrok_native_http_tunnel(self.env);

        let sess: Result<MutexGuard<'_, Session>, _> =
            self.env.get_rust_field(this, "native_address");
        match sess {
            Ok(mu) => {
                let mut bldr = mu.http_endpoint();

                match self.get_string_field(jtb, "metadata") {
                    Ok(Some(metadata)) => {
                        bldr = bldr.metadata(metadata);
                    }
                    Ok(None) => {}
                    Err(err) => {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    }
                }

                match self.get_string_field(jtb, "domain") {
                    Ok(Some(domain)) => {
                        bldr = bldr.domain(domain);
                    }
                    Ok(None) => {}
                    Err(err) => {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    }
                }

                let tun = rt.block_on(bldr.listen());
                match tun {
                    Ok(tun) => {
                        self.set_string_field(jtunnel, "id", tun.id())
                            .expect("could not set id");
                        self.set_string_field(jtunnel, "forwardsTo", tun.forwards_to())
                            .expect("could not set forwards_to");
                        self.set_string_field(jtunnel, "metadata", tun.metadata())
                            .expect("could not set metadata");
                        self.set_string_field(jtunnel, "proto", "http")
                            .expect("could not set proto");
                        self.set_string_field(jtunnel, "url", tun.url())
                            .expect("could not set url");

                        self.env
                            .set_rust_field(jtunnel, "native_address", tun)
                            .unwrap_or_else(|err| {
                                self.env
                                    .throw(err.to_string())
                                    .expect("could not throw exception");
                            })
                    }
                    Err(err) => {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    }
                }
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(jtunnel)
    }

    fn labeled_tunnel(
        &self,
        this: ComNgrokNativeSession<'local>,
        jtb: ComNgrokLabeledTunnelBuilder<'local>,
    ) -> Result<ComNgrokNativeLabeledTunnel<'local>, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let jtunnel = ComNgrokNativeLabeledTunnel::new_1com_ngrok_native_labeled_tunnel(self.env);

        let sess: Result<MutexGuard<'_, Session>, _> =
            self.env.get_rust_field(this, "native_address");
        match sess {
            Ok(mu) => {
                let mut bldr = mu.labeled_tunnel();

                match self.get_string_field(jtb, "metadata") {
                    Ok(Some(metadata)) => {
                        bldr = bldr.metadata(metadata);
                    }
                    Ok(None) => {}
                    Err(err) => {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    }
                }

                {
                    let labels = self
                        .env
                        .call_method(jtb, "labels", "()Ljava/util/List;", &[])
                        .and_then(|o| o.l())
                        .expect("could not get labels");
                    let labels_count = self
                        .env
                        .call_method(labels, "size", "()I", &[])
                        .and_then(|o| o.i())
                        .expect("could not get labels size");
                    for i in 0..labels_count {
                        let label: ComNgrokLabeledTunnelLabel = self
                            .env
                            .call_method(labels, "get", "(I)Ljava/lang/Object;", &[JValue::Int(i)])
                            .and_then(|o| o.l())
                            .expect("could not get version object")
                            .into();
                        bldr = bldr.label(label.name(self.env), label.value(self.env));
                    }
                }

                let tun = rt.block_on(bldr.listen());
                match tun {
                    Ok(tun) => {
                        self.set_string_field(jtunnel, "id", tun.id())
                            .expect("could not set id");
                        self.set_string_field(jtunnel, "forwardsTo", tun.forwards_to())
                            .expect("could not set forwards_to");
                        self.set_string_field(jtunnel, "metadata", tun.metadata())
                            .expect("could not set metadata");

                        self.env
                            .set_rust_field(jtunnel, "native_address", tun)
                            .unwrap_or_else(|err| {
                                self.env
                                    .throw(err.to_string())
                                    .expect("could not throw exception");
                            })
                    }
                    Err(err) => {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    }
                }
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(jtunnel)
    }

    fn close(
        &self,
        this: ComNgrokNativeSession<'local>,
    ) -> Result<(), jaffi_support::Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let sess: Result<Session, _> = self.env.take_rust_field(this, "native_address");
        match sess {
            Ok(mut sess) => {
                let closed = rt.block_on(sess.close());
                match closed {
                    Ok(_) => {}
                    Err(err) => {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    }
                }
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(())
    }
}

struct NativeTcpTunnelRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> SetStringField<'local> for NativeTcpTunnelRsImpl<'local> {
    fn get_env(&self) -> JNIEnv<'local> {
        self.env
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

        let jconn = ComNgrokNativeConnection::new_1com_ngrok_native_connection(self.env);

        let tun: Result<MutexGuard<'_, TcpTunnel>, _> =
            self.env.get_rust_field(this, "native_address");
        match tun {
            Ok(mut mu) => {
                let res: Result<Option<Conn>, _> = rt.block_on(mu.try_next());
                let conn = res.expect("conn result err").expect("conn option err");

                self.set_string_field(jconn, "remoteAddr", &conn.remote_addr().to_string())
                    .expect("cannot set remoteAddr");

                self.env
                    .set_rust_field(jconn, "native_address", conn)
                    .unwrap_or_else(|err| {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    })
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(jconn)
    }

    fn forward_tcp(&self, this: ComNgrokNativeTcpTunnel<'local>, addr: String) {
        let rt = RT.get().expect("runtime not initialized");

        let tun: Result<MutexGuard<'_, TcpTunnel>, _> =
            self.env.get_rust_field(this, "native_address");
        match tun {
            Ok(mut mu) => {
                let res = rt.block_on(mu.forward_tcp(addr));
                match res {
                    Ok(()) => {}
                    Err(err) => self
                        .env
                        .throw(err.to_string())
                        .expect("could not throw exception"),
                }
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }
    }

    fn close(&self, this: ComNgrokNativeTcpTunnel<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let tun: Result<TcpTunnel, _> = self.env.take_rust_field(this, "native_address");
        match tun {
            Ok(mut mu) => rt.block_on(mu.close()).expect("could not close"),
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(())
    }
}

struct NativeTlsTunnelRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> SetStringField<'local> for NativeTlsTunnelRsImpl<'local> {
    fn get_env(&self) -> JNIEnv<'local> {
        self.env
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

        let jconn = ComNgrokNativeConnection::new_1com_ngrok_native_connection(self.env);

        let tun: Result<MutexGuard<'_, TlsTunnel>, _> =
            self.env.get_rust_field(this, "native_address");
        match tun {
            Ok(mut mu) => {
                let res: Result<Option<Conn>, _> = rt.block_on(mu.try_next());
                let conn = res.expect("conn result err").expect("conn option err");

                self.set_string_field(jconn, "remoteAddr", &conn.remote_addr().to_string())
                    .expect("cannot set remoteAddr");

                self.env
                    .set_rust_field(jconn, "native_address", conn)
                    .unwrap_or_else(|err| {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    })
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(jconn)
    }

    fn forward_tcp(&self, this: ComNgrokNativeTlsTunnel<'local>, addr: String) {
        let rt = RT.get().expect("runtime not initialized");

        let tun: Result<MutexGuard<'_, TlsTunnel>, _> =
            self.env.get_rust_field(this, "native_address");
        match tun {
            Ok(mut mu) => {
                let res = rt.block_on(mu.forward_tcp(addr));
                match res {
                    Ok(()) => {}
                    Err(err) => self
                        .env
                        .throw(err.to_string())
                        .expect("could not throw exception"),
                }
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }
    }

    fn close(&self, this: ComNgrokNativeTlsTunnel<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let tun: Result<TlsTunnel, _> = self.env.take_rust_field(this, "native_address");
        match tun {
            Ok(mut mu) => rt
                .block_on(async { mu.close().await })
                .expect("could not close"),
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(())
    }
}

struct NativeHttpTunnelRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> SetStringField<'local> for NativeHttpTunnelRsImpl<'local> {
    fn get_env(&self) -> JNIEnv<'local> {
        self.env
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

        let jconn = ComNgrokNativeConnection::new_1com_ngrok_native_connection(self.env);

        let tun: Result<MutexGuard<'_, HttpTunnel>, _> =
            self.env.get_rust_field(this, "native_address");
        match tun {
            Ok(mut mu) => {
                let res: Result<Option<Conn>, _> = rt.block_on(mu.try_next());
                let conn = res.expect("conn result err").expect("conn option err");

                self.set_string_field(jconn, "remoteAddr", &conn.remote_addr().to_string())
                    .expect("cannot set remoteAddr");

                self.env
                    .set_rust_field(jconn, "native_address", conn)
                    .unwrap_or_else(|err| {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    })
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(jconn)
    }

    fn forward_tcp(&self, this: ComNgrokNativeHttpTunnel<'local>, addr: String) {
        let rt = RT.get().expect("runtime not initialized");

        let tun: Result<MutexGuard<'_, HttpTunnel>, _> =
            self.env.get_rust_field(this, "native_address");
        match tun {
            Ok(mut mu) => {
                let res = rt.block_on(mu.forward_tcp(addr));
                match res {
                    Ok(()) => {}
                    Err(err) => self
                        .env
                        .throw(err.to_string())
                        .expect("could not throw exception"),
                }
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }
    }

    fn close(
        &self,
        this: ComNgrokNativeHttpTunnel<'local>,
    ) -> Result<(), jaffi_support::Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let tun: Result<HttpTunnel, _> = self.env.take_rust_field(this, "native_address");
        match tun {
            Ok(mut mu) => rt.block_on(mu.close()).expect("could not close"),
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(())
    }
}

struct NativeLabeledTunnelRsImpl<'local> {
    env: JNIEnv<'local>,
}

impl<'local> SetStringField<'local> for NativeLabeledTunnelRsImpl<'local> {
    fn get_env(&self) -> JNIEnv<'local> {
        self.env
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

        let jconn = ComNgrokNativeConnection::new_1com_ngrok_native_connection(self.env);

        let tun: Result<MutexGuard<'_, LabeledTunnel>, _> =
            self.env.get_rust_field(this, "native_address");
        match tun {
            Ok(mut mu) => {
                let res: Result<Option<Conn>, _> = rt.block_on(mu.try_next());
                let conn = res.expect("conn result err").expect("conn option err");

                self.set_string_field(jconn, "remoteAddr", &conn.remote_addr().to_string())
                    .expect("cannot set remoteAddr");

                self.env
                    .set_rust_field(jconn, "native_address", conn)
                    .unwrap_or_else(|err| {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    })
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(jconn)
    }

    fn forward_tcp(&self, this: ComNgrokNativeLabeledTunnel<'local>, addr: String) {
        let rt = RT.get().expect("runtime not initialized");

        let tun: Result<MutexGuard<'_, LabeledTunnel>, _> =
            self.env.get_rust_field(this, "native_address");
        match tun {
            Ok(mut mu) => {
                let res = rt.block_on(mu.forward_tcp(addr));
                match res {
                    Ok(()) => {}
                    Err(err) => self
                        .env
                        .throw(err.to_string())
                        .expect("could not throw exception"),
                }
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }
    }

    fn close(
        &self,
        this: ComNgrokNativeLabeledTunnel<'local>,
    ) -> Result<(), jaffi_support::Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let tun: Result<LabeledTunnel, _> = self.env.take_rust_field(this, "native_address");
        match tun {
            Ok(mut mu) => rt.block_on(mu.close()).expect("could not close"),
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(())
    }
}

struct NativeConnectionRsImpl<'local> {
    env: JNIEnv<'local>,
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

        let mut readsz: i32 = 0;
        let conn: Result<MutexGuard<'_, Conn>, _> = self.env.get_rust_field(this, "native_address");
        match conn {
            Ok(mut mu) => {
                let addr = self
                    .env
                    .get_direct_buffer_address(jbuff)
                    .expect("cannot get buff addr");
                let res = rt.block_on(mu.read(addr));
                match res {
                    Ok(0) => {
                        return Err(Error::new(
                            IOExceptionErr::IOException(IOException),
                            "closed",
                        ));
                    }
                    Ok(sz) => {
                        readsz = sz.try_into().expect("cannot convert to i32");
                    }
                    Err(err) => {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    }
                }
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(readsz)
    }

    fn write_native(
        &self,
        this: ComNgrokNativeConnection<'local>,
        jbuff: JByteBuffer<'local>,
        limit: i32,
    ) -> Result<i32, Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let mut writesz: i32 = 0;
        let conn: Result<MutexGuard<'_, Conn>, _> = self.env.get_rust_field(this, "native_address");
        match conn {
            Ok(mut mu) => {
                let addr = self
                    .env
                    .get_direct_buffer_address(jbuff)
                    .expect("cannot get buff addr");
                let act = &addr[..limit.try_into().expect("xxx")];
                let res = rt.block_on(mu.write(act));
                match res {
                    Ok(sz) => {
                        writesz = sz.try_into().expect("cannot convert to i32");
                    }
                    Err(err) => {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    }
                }
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(writesz)
    }

    fn close(&self, this: ComNgrokNativeConnection<'local>) -> Result<(), Error<IOExceptionErr>> {
        let rt = RT.get().expect("runtime not initialized");

        let conn: Result<Conn, _> = self.env.take_rust_field(this, "native_address");
        match conn {
            Ok(mut mu) => {
                let res = rt.block_on(mu.shutdown());
                match res {
                    Ok(()) => {}
                    Err(err) => {
                        self.env
                            .throw(err.to_string())
                            .expect("could not throw exception");
                    }
                }
            }
            Err(err) => {
                self.env
                    .throw(err.to_string())
                    .expect("could not throw exception");
            }
        }

        Ok(())
    }
}
