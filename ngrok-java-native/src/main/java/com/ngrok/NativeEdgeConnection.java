package com.ngrok;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * An implementation of {@link Connection.Edge} that delegates implementation to a native library.
 */
public class NativeEdgeConnection implements Connection.Edge {
    private long native_address;

    private final String remoteAddr;
    private final String edgeType;
    private final boolean passthroughTls;

    public NativeEdgeConnection(String remoteAddr, String edgeType, boolean passthroughTls) {
        this.remoteAddr = remoteAddr;
        this.edgeType = edgeType;
        this.passthroughTls = passthroughTls;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public String getEdgeType() {
        return edgeType;
    }

    @Override
    public boolean isPassthroughTls() {
        return passthroughTls;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        var sz = readNative(dst);
        dst.position(0);
        dst.limit(sz);
        return sz;
    }

    private native int readNative(ByteBuffer dst) throws IOException;

    @Override
    public int write(ByteBuffer src) throws IOException {
        return writeNative(src, src.limit());
    }

    private native int writeNative(ByteBuffer src, int limit) throws IOException;

    @Override
    public native void close() throws IOException;
}
