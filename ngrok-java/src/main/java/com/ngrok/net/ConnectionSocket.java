package com.ngrok.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * A socket for establishing a connection to a remote server.
 */
public class ConnectionSocket extends Socket {
    /**
     * Creates a new connection socket.
     *
     * @throws IOException if an I/O error occurs
     */
    protected ConnectionSocket() throws IOException {
        super(new ConnectionSocketImpl());
    }

    /**
     * Returns the {@link InetAddress} of the remote endpoint of this socket.
     *
     * @return the {@link InetAddress} of the remote endpoint
     */
    @Override
    public InetAddress getInetAddress() {
        return super.getInetAddress();
    }
}