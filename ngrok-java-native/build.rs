use std::{
    borrow::Cow,
    error::Error,
    io::Write,
    path::{Path, PathBuf},
    process::Command,
};

use jaffi::Jaffi;

fn main() -> Result<(), Box<dyn Error>> {
    let class_path = vec![
        Cow::from(PathBuf::from("../ngrok-java/target/classes")),
        Cow::from(PathBuf::from("target/classes")),
    ];
    let classes = vec![
        Cow::from("com.ngrok.Runtime"),
        Cow::from("com.ngrok.NativeSession"),
        Cow::from("com.ngrok.NativeTcpListener"),
        Cow::from("com.ngrok.NativeTcpForwarder"),
        Cow::from("com.ngrok.NativeTlsListener"),
        Cow::from("com.ngrok.NativeTlsForwarder"),
        Cow::from("com.ngrok.NativeHttpListener"),
        Cow::from("com.ngrok.NativeHttpForwarder"),
        Cow::from("com.ngrok.NativeEdgeListener"),
        Cow::from("com.ngrok.NativeEdgeForwarder"),
        Cow::from("com.ngrok.NativeEdgeConnection"),
        Cow::from("com.ngrok.NativeEndpointConnection"),
    ];
    let classes_to_wrap = vec![
        Cow::from("com.ngrok.Runtime$Logger"),
        Cow::from("com.ngrok.Session"),
        Cow::from("com.ngrok.Session$Builder"),
        Cow::from("com.ngrok.Session$ClientInfo"),
        Cow::from("com.ngrok.Session$CommandHandler"),
        Cow::from("com.ngrok.Session$HeartbeatHandler"),
        Cow::from("com.ngrok.MetadataBuilder"),
        Cow::from("com.ngrok.EdgeBuilder"),
        Cow::from("com.ngrok.EndpointBuilder"),
        Cow::from("com.ngrok.HttpBuilder"),
        Cow::from("com.ngrok.Http$Header"),
        Cow::from("com.ngrok.Http$BasicAuth"),
        Cow::from("com.ngrok.Http$OAuth"),
        Cow::from("com.ngrok.Http$OIDC"),
        Cow::from("com.ngrok.Http$WebhookVerification"),
        Cow::from("com.ngrok.TcpBuilder"),
        Cow::from("com.ngrok.TlsBuilder"),
        Cow::from("com.ngrok.AbstractEdge"),
        Cow::from("com.ngrok.AbstractEndpoint"),
        Cow::from("com.ngrok.NgrokException"),
    ];
    let output_dir = PathBuf::from(std::env::var("OUT_DIR").expect("OUT_DIR not set"));

    let jaffi = Jaffi::builder()
        .classpath(class_path)
        .classes_to_wrap(classes_to_wrap)
        .native_classes(classes)
        .output_dir(&output_dir)
        .build();

    jaffi.generate()?;

    let output_file = Cow::from(Path::new("generated_jaffi.rs"));
    let jaffi_file = output_dir.join(output_file);

    let mut cmd = Command::new("rustfmt");
    cmd.arg("--emit").arg("files").arg(jaffi_file);

    eprintln!("cargo fmt: {cmd:?}");
    let output = cmd.output();

    match output {
        Ok(output) => {
            std::io::stderr().write_all(&output.stdout)?;
            std::io::stderr().write_all(&output.stderr)?;
        }
        Err(e) => {
            eprintln!("cargo fmt failed to execute: {e}");
        }
    }

    Ok(())
}
