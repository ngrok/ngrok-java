package com.ngrok;

import java.io.IOException;
import java.util.Map;

public class NativeEdgeListener extends AbstractEdge implements Listener.Edge {
    private long native_address;

    public NativeEdgeListener(String id, String metadata, String forwardsTo, Map<String, String> labels) {
        super(id, metadata, forwardsTo, labels);
    }

    @Override
    public native NativeEdgeConnection accept() throws IOException;

    @Override
    public native void close() throws IOException;
}
