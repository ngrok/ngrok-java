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
        Cow::from("com.ngrok.NativeTcpTunnel"),
        Cow::from("com.ngrok.NativeTlsTunnel"),
        Cow::from("com.ngrok.NativeHttpTunnel"),
        Cow::from("com.ngrok.NativeLabeledTunnel"),
        Cow::from("com.ngrok.NativeConnection"),
    ];
    let classes_to_wrap = vec![
        Cow::from("com.ngrok.Runtime$Logger"),
        Cow::from("com.ngrok.Session"),
        Cow::from("com.ngrok.Session$Builder"),
        Cow::from("com.ngrok.Session$UserAgent"),
        Cow::from("com.ngrok.Session$StopCallback"),
        Cow::from("com.ngrok.Session$RestartCallback"),
        Cow::from("com.ngrok.Session$UpdateCallback"),
        Cow::from("com.ngrok.Session$HeartbeatHandler"),
        Cow::from("com.ngrok.TcpTunnel"),
        Cow::from("com.ngrok.TcpTunnel$Builder"),
        Cow::from("com.ngrok.TlsTunnel"),
        Cow::from("com.ngrok.TlsTunnel$Builder"),
        Cow::from("com.ngrok.HttpTunnel"),
        Cow::from("com.ngrok.HttpTunnel$Builder"),
        Cow::from("com.ngrok.LabeledTunnel"),
        Cow::from("com.ngrok.LabeledTunnel$Label"),
        Cow::from("com.ngrok.LabeledTunnel$Builder"),
        Cow::from("com.ngrok.Connection"),
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
