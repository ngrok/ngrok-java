package com.ngrok;

import java.io.IOException;
import java.nio.ByteBuffer;

public class NativeConnection extends Connection {
    private long native_address;

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
