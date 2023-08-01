package com.ngrok;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 *  Represents a connection established over a tunnel.
 */
public abstract class Connection implements AutoCloseable {
    private final String remoteAddr;

    /**
     * Creates a new connection with the given remote address.
     *
     * @param remoteAddr the remote address to connect to
     */
    public Connection(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    /**
     * Retrieves the remote address of the connection.
     *
     * @return the remote address of the connection
     */
    public String getRemoteAddr() {
        return remoteAddr;
    }

    /**
     * Creates an {@link InetSocketAddress} for this connection's remote address.
     *
     * @return {@link InetSocketAddress} representing the internet address
     */
    public InetSocketAddress inetAddress() {
        var parts = remoteAddr.split(":");
        return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
    }

    /**
     * Reads the next available bytes from this connection, up to the buffer capacity.
     *
     * @param dst the buffer to read bytes into
     * @return the number of bytes read, or -1 if the end of the stream has been reached
     * @throws IOException if an I/O error occurs
     */
    public abstract int read(ByteBuffer dst) throws IOException;

    /**
     * Writes a sequence of bytes to this connection from the given buffer.
     *
     * @param src the buffer containing bytes to write
     * @return the number of bytes written
     * @throws IOException if an I/O error occurs
     */
    public abstract int write(ByteBuffer src) throws IOException;

    /**
     * Closes this connection and releases any system resources associated with it.
     *
     * @throws IOException if an I/O error occurs
     */
    public abstract void close() throws IOException;
}