package com.ngrok;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A class representing a connection for an HTTP tunnel with native implementation.
 */
public class NativeConnection extends Connection {
    private long native_address;

    /**
     * Constructs a new connection with the specified remote address.
     *
     * @param remoteAddr the remote address of the connection
     */
    public NativeConnection(String remoteAddr) {
        super(remoteAddr);
    }

    /**
     * Reads data from the connection into the specified buffer.
     *
     * @param dst the buffer to read data into
     * @return the number of bytes read
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(ByteBuffer dst) throws IOException {
        var sz = readNative(dst);
        dst.position(0);
        dst.limit(sz);
        return sz;
    }

    /**
     * Reads data from the native connection into the specified buffer using native
     * code.
     *
     * @param dst the buffer to read data into
     * @return the number of bytes read
     * @throws IOException if an I/O error occurs
     */
    private native int readNative(ByteBuffer dst) throws IOException;

    /**
     * Writes data from the specified buffer to the connection.
     *
     * @param src the buffer to write data from
     * @return the number of bytes written
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int write(ByteBuffer src) throws IOException {
        return writeNative(src, src.limit());
    }

    /**
     * Writes data from the specified buffer to the native connection using native
     * code.
     *
     * @param src   the buffer to write data from
     * @param limit the limit of the buffer
     * @return the number of bytes written
     * @throws IOException if an I/O error occurs
     */
    private native int writeNative(ByteBuffer src, int limit) throws IOException;

    /**
     * Closes the connection.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public native void close() throws IOException;
}
