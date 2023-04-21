package com.ngrok;

import java.io.IOException;
import java.util.Properties;

public class NativeSession implements Session {
    private static String version = "0.0.0-UNKNOWN";

    static {
        try {
            Runtime.load();
            Runtime.init(new Runtime.Logger());
        } catch (Throwable th) {
            // TODO better error handling here?
            th.printStackTrace();
        }

        try {
            Properties props = new Properties();
            props.load(NativeSession.class.getResourceAsStream("/native.properties"));
            version = props.getProperty("agent.version", "0.0.0-SNAPSHOT");
        } catch (Throwable th) {
            // TODO better error handling here?
            th.printStackTrace();
        }
    }

    private long native_address;

    private String metadata;

    public static NativeSession connect(Session.Builder builder) throws IOException {
        builder.getUserAgents().add(0, new UserAgent("ngrok-java", version));
        return connectNative(builder);
    }

    public static native NativeSession connectNative(Session.Builder builder) throws IOException;

    public String getMetadata() {
        return metadata;
    }

    public native NativeTcpTunnel tcpTunnel(TcpTunnel.Builder builder) throws IOException;

    public native NativeTlsTunnel tlsTunnel(NativeTlsTunnel.Builder builder) throws IOException;

    public native NativeHttpTunnel httpTunnel(NativeHttpTunnel.Builder builder) throws IOException;

    public native NativeLabeledTunnel labeledTunnel(NativeLabeledTunnel.Builder builder) throws IOException;

    public native void close() throws IOException;
}
