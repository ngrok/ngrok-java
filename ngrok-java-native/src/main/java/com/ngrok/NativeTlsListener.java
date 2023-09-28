package com.ngrok;

import java.io.IOException;

public class NativeTlsListener extends AbstractEndpoint implements Listener.Endpoint {
    private long native_address;

    public NativeTlsListener(String id, String metadata, String forwardsTo, String proto, String url) {
        super(id, metadata, forwardsTo, proto, url);
    }

    @Override
    public native NativeEndpointConnection accept() throws IOException;

    @Override
    public native void close() throws IOException;
}
