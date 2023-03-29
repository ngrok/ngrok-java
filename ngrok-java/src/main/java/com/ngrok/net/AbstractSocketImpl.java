package com.ngrok.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;

public class AbstractSocketImpl extends SocketImpl {
    @Override
    protected void create(boolean stream) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void listen(int backlog) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int available() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOption(int optID, Object value) throws SocketException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getOption(int optID) throws SocketException {
        throw new UnsupportedOperationException();
    }
}
