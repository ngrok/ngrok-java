package com.ngrok.net;

import com.ngrok.Connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An implementation of {@link AbstractSocketImpl} for establishing a socket connection to a remote server.
 */
public class ConnectionSocketImpl extends AbstractSocketImpl {
    protected Connection connection;

    protected ConnectionSocketImpl() { }

    protected void setConnection(Connection connection) {
        this.connection = connection;

        var addr = connection.inetAddress();
        this.address = addr.getAddress();
        this.port = addr.getPort();
    }

    /**
     * Creates and returns a {@link ConnectionInputStream} for reading data from the connection.
     *
     * @return an {@link InputStream} for reading data
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected InputStream getInputStream() throws IOException {
        return new ConnectionInputStream(connection, 1024);
    }

    /**
     * Creates and returns a {@link ConnectionOutputStream} for writing data to the connection.
     *
     * @return an {@link OutputStream} for writing data
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected OutputStream getOutputStream() throws IOException {
        return new ConnectionOutputStream(connection, 1024);
    }
}