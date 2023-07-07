package com.ngrok.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;


/**
 * Abstract implementation of the {@link SocketImpl} interface.
 */
public class AbstractSocketImpl extends SocketImpl {
    /**
     * See {@link SocketImpl#create(boolean)} 
     */
    @Override
    protected void create(boolean stream) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link SocketImpl#connect(String, int)}
     */
    @Override
    protected void connect(String host, int port) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link SocketImpl#connect(InetAddress, int)}
     */
    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link SocketImpl#connect(SocketAddress, int)}
     */
    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link SocketImpl#bind(InetAddress, int)}
     */
    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link SocketImpl#listen(int)}
     */
    @Override
    protected void listen(int backlog) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link SocketImpl#accept(SocketImpl)}
     */
    @Override
    protected void accept(SocketImpl s) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link SocketImpl#getInputStream()}
     */
    @Override
    protected InputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link SocketImpl#getOutputStream()}
     */
    @Override
    protected OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link SocketImpl#available()}
     */
    @Override
    protected int available() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link SocketImpl#close()}
     */
    @Override
    protected void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link SocketImpl#sendUrgentData(int)}
     */
    @Override
    protected void sendUrgentData(int data) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link SocketImpl#setOption(int, Object)}
     */
    @Override
    public void setOption(int optID, Object value) throws SocketException {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link SocketImpl#getOption(int)}
     */
    @Override
    public Object getOption(int optID) throws SocketException {
        throw new UnsupportedOperationException();
    }
}
