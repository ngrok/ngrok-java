package com.ngrok.net;

import com.ngrok.Connection;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * An output stream for writing data to an ngrok connection.
 */
public class ConnectionOutputStream extends OutputStream {
    private final Connection connection;

    private final ByteBuffer buffer;

    /**
     * Creates a new output stream for the given connection with the specified buffer size.
     *
     * @param connection the connection to write to
     * @param bufferSize the size of the buffer to use to write data to the connection
     */
    public ConnectionOutputStream(Connection connection, int bufferSize) {
        this.connection = connection;
        this.buffer = ByteBuffer.allocateDirect(bufferSize);
    }

    /**
     * Writes a single byte of data to the output stream.
     *
     * @param b the byte to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(int b) throws IOException {
        buffer.put((byte) b);
        flush();
    }

    /**
     * Writes bytes from the specified byte array starting at offset off to the output stream.
     *
     * @param b the array of bytes to write
     * @param off the offset within the buffer to start writing from
     * @param len the number of bytes to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        buffer.put(b, off, len);
        flush();
    }

    /**
     * Flushes the output stream, forcing any buffered output bytes to be written out.
     * Automatically called by {@link #write(int)} and {@link #write(byte[], int, int)}.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
        buffer.flip();
        connection.write(buffer);
        buffer.compact();
    }
}