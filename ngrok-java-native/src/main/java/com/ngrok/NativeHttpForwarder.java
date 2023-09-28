package com.ngrok;

import java.io.IOException;

public class NativeHttpForwarder extends AbstractEndpoint implements Forwarder.Endpoint {
    private long native_address;

    public NativeHttpForwarder(String id, String metadata, String forwardsTo, String proto, String url) {
        super(id, metadata, forwardsTo, proto, url);
    }

    @Override
    public native void join() throws IOException;

    @Override
    public native void close() throws IOException;
}
