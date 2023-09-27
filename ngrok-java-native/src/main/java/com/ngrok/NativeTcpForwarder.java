package com.ngrok;

import java.io.IOException;

public class NativeTcpForwarder implements Tcp.Forwarder {
    private long native_address;

    private final EndpointInfo info;

    public NativeTcpForwarder(EndpointInfo info) {
        this.info = info;
    }

    @Override
    public EndpointInfo info() {
        return info;
    }

    @Override
    public native void join() throws IOException;

    @Override
    public native void close() throws IOException;
}
