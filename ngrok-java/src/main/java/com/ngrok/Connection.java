package com.ngrok;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 *  Represents a connection established over a tunnel.
 */
public interface Connection extends AutoCloseable {
    String getRemoteAddr();

    default InetSocketAddress inetAddress() {
        var parts = getRemoteAddr().split(":");
        return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
    }

    /**
     * Reads the next available bytes from this connection, up to the buffer capacity.
     *
     * @param dst the buffer to read bytes into
     * @return the number of bytes read, or -1 if the end of the stream has been reached
     * @throws IOException if an I/O error occurs
     */
    int read(ByteBuffer dst) throws IOException;

    /**
     * Writes a sequence of bytes to this connection from the given buffer.
     *
     * @param src the buffer containing bytes to write
     * @return the number of bytes written
     * @throws IOException if an I/O error occurs
     */
    int write(ByteBuffer src) throws IOException;

    /**
     * Closes this connection and releases any system resources associated with it.
     *
     * @throws IOException if an I/O error occurs
     */
    void close() throws IOException;

    interface Endpoint extends Connection {
        String getProto();
    }

    interface Edge extends Connection {
        String getEdgeType();

        boolean isPassthroughTls();
    }
}