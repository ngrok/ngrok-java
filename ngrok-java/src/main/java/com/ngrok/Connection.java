package com.ngrok;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Represents a connection established over a listener.
 */
public interface Connection extends AutoCloseable {
    /**
     * Returns the remote address that established this connection.
     *
     * @return an internet address, in IP:port form
     */
    String getRemoteAddr();

    /**
     * Creates an {@link InetSocketAddress} for this connection's remote address.
     *
     * @return {@link InetSocketAddress} representing the internet address
     */
    default InetSocketAddress inetAddress() {
        var addr = getRemoteAddr();
        var lastColumn = addr.lastIndexOf(":");
        var host = addr.substring(0, lastColumn);
        var port = addr.substring(lastColumn + 1);
        return new InetSocketAddress(host, Integer.parseInt(port));
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

    /**
     * Represents a connection establish over an endpoint listener.
     */
    interface Endpoint extends Connection {
        /**
         * Returns the protocol of this connection.
         *
         * @return the protocol, for example {@code http} or {@code tcp}
         */
        String getProto();
    }

    /**
     * Represents a connection established over an edge listener
     */
    interface Edge extends Connection {
        /**
         * Returns the edge type for this connection.
         *
         * @return the edge type, for example {@code https} or {@code tcp}
         */
        String getEdgeType();

        /**
         * Returns if this connection is passthrough TLS connection
         *
         * @return true if passthrough TLS, false otherwise
         */
        boolean isPassthroughTls();
    }
}