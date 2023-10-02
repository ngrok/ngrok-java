package com.ngrok.net;

import com.ngrok.Connection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * An input stream for reading data from {@link Connection}.
 */
public class ConnectionInputStream extends InputStream {
    private final Connection connection;

    private final ByteBuffer buffer;

    /**
     * Creates a new input stream for the given connection with the specified buffer
     * size.
     *
     * @param connection the connection to read from
     * @param bufferSize the size of the buffer to use to read data from the
     *                   connection
     */
    public ConnectionInputStream(Connection connection, int bufferSize) {
        this.connection = connection;
        this.buffer = ByteBuffer.allocateDirect(bufferSize);
        this.buffer.flip();
    }

    /**
     * Prepares the buffer for reading by clearing it and then reading data from the
     * connection into the buffer. Ignored if the buffer is not empty.
     * Automatically called by {@link #read()} and {@link #read(byte[], int, int)}.
     * 
     * @throws IOException if an I/O error occurs
     */
    private void prepare() throws IOException {
        if (buffer.hasRemaining()) {
            return;
        }

        buffer.clear();
        connection.read(buffer);
    }

    /**
     * Reads a single byte of data from the input stream.
     *
     * @return the byte of data, or -1 if the end of the stream has been reached
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        prepare();
        return buffer.get();
    }

    /**
     * Reads up to len bytes of data from the input stream into an array of bytes.
     *
     * @param b   the array of bytes to read the data into
     * @param off the offset within the buffer to start reading the data from
     * @param len the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or -1 if the end of
     *         the stream has been reached
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        prepare();

        var readLen = Math.min(len, buffer.remaining());
        buffer.get(b, off, readLen);
        return readLen;
    }
}