package com.ngrok;

import java.io.IOException;

/**
 * An implementation of {@link Forwarder.Endpoint} that delegates implementation to a native library.
 */
public class NativeTlsForwarder extends AbstractEndpoint implements Forwarder.Endpoint {
    private long native_address;

    public NativeTlsForwarder(String id, String metadata, String forwardsTo, String proto, String url) {
        super(id, metadata, forwardsTo, proto, url);
    }

    @Override
    public native void join() throws IOException;

    @Override
    public native void close() throws IOException;
}
