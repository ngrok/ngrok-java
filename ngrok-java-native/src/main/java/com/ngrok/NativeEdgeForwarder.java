package com.ngrok;

import java.io.IOException;
import java.util.Map;

/**
 * An implementation of {@link Forwarder.Edge} that delegates implementation to a native library.
 */
public class NativeEdgeForwarder extends AbstractEdge implements Forwarder.Edge {
    private long native_address;

    public NativeEdgeForwarder(String id, String metadata, String forwardsTo, Map<String, String> labels) {
        super(id, metadata, forwardsTo, labels);
    }

    @Override
    public native void join() throws IOException;

    @Override
    public native void close() throws IOException;
}
