package com.ngrok.net;

import com.ngrok.Connection;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ConnectionOutputStream extends OutputStream {
    private final Connection connection;

    private final ByteBuffer buffer;

    public ConnectionOutputStream(Connection connection, int bufferSize) {
        this.connection = connection;
        this.buffer = ByteBuffer.allocateDirect(bufferSize);
    }

    @Override
    public void write(int b) throws IOException {
        buffer.put((byte) b);
        flush();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int pos = 0, delta = Math.min(buffer.capacity(), len); pos < len; pos += delta) {
            delta = Math.min(delta, len-pos);
            buffer.put(b, off+pos, delta);
            flush();
        }
    }

    @Override
    public void flush() throws IOException {
        buffer.flip();
        connection.write(buffer);
        buffer.clear();
    }
}
