package com.ngrok;

import java.io.IOException;
public class NativeHttpTunnel extends HttpTunnel {
    private long native_address;

    public NativeHttpTunnel(String id, String forwardsTo, String metadata, String proto, String url) {
        super(id, forwardsTo, metadata, proto, url);
    }

    @Override
    public native NativeConnection accept() throws IOException;

    @Override
    public native void forwardTcp(String addr) throws IOException;

    @Override
    public native void close() throws IOException;
}
