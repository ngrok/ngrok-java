package com.ngrok;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * An implementation of {@link Connection.Endpoint} that delegates implementation to a native library.
 */
public class NativeEndpointConnection implements Connection.Endpoint {
    private long native_address;

    private final String proto;

    private final String remoteAddr;

    public NativeEndpointConnection(String remoteAddr, String proto) {
        this.proto = proto;
        this.remoteAddr = remoteAddr;
    }

    public String getProto() {
        return proto;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr;
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

    public native void close() throws IOException;
}
