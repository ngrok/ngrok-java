package com.ngrok;

import java.io.IOException;

public class NativeSession implements Session {
    static {
        try {
            Runtime.load();
            Runtime.init(new Runtime.Logger());
        } catch (Throwable th) {
            // TODO better error handling here?
            th.printStackTrace();
        }
    }

    private long native_address;

    private String metadata;

    public static native NativeSession connect(Session.Builder builder) throws IOException;

    public String metadata() {
        return metadata;
    }

    public native NativeTcpTunnel tcpTunnel(TcpTunnel.Builder builder) throws IOException;

    public native NativeTlsTunnel tlsTunnel(NativeTlsTunnel.Builder builder) throws IOException;

    public native NativeHttpTunnel httpTunnel(NativeHttpTunnel.Builder builder) throws IOException;

    public native NativeLabeledTunnel labeledTunnel(NativeLabeledTunnel.Builder builder) throws IOException;

    public native void close() throws IOException;
}
