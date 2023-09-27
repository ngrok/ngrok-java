package com.ngrok;

import java.io.IOException;

public class NativeTcpListener implements Tcp.Listener {
    private long native_address;

    private final EndpointInfo info;

    public NativeTcpListener(EndpointInfo info) {
        this.info = info;
    }

    @Override
    public EndpointInfo info() {
        return info;
    }

    @Override
    public native NativeEndpointConnection accept() throws IOException;

    @Override
    public native void close() throws IOException;
}
