package com.ngrok.net;

import com.ngrok.Connection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ConnectionInputStream extends InputStream {
    private final Connection connection;

    private final ByteBuffer buffer;

    public ConnectionInputStream(Connection connection, int bufferSize) {
        this.connection = connection;
        this.buffer = ByteBuffer.allocateDirect(bufferSize);
        this.buffer.flip();
    }

    private void prepare() throws IOException {
        if (buffer.hasRemaining()) {
            return;
        }

        buffer.clear();
        connection.read(buffer);
    }

    @Override
    public int read() throws IOException {
        prepare();

        return buffer.get();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        prepare();

        var readLen = Math.min(len, buffer.remaining());
        buffer.get(b, off, readLen);
        return readLen;
    }
}
