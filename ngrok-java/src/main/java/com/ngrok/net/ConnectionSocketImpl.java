package com.ngrok.net;

import com.ngrok.Connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectionSocketImpl extends AbstractSocketImpl {
    protected Connection connection;

    protected ConnectionSocketImpl() {
    }

    protected void setConnection(Connection connection) {
        this.connection = connection;

        var addr = connection.inetAddress();
        this.address = addr.getAddress();
        this.port = addr.getPort();
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return new ConnectionInputStream(connection, 4096);
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        return new ConnectionOutputStream(connection, 4096);
    }
}
