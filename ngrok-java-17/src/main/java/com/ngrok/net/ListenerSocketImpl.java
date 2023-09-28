package com.ngrok.net;

import com.ngrok.Listener;

import java.io.IOException;
import java.net.SocketImpl;

/**
 * An implementation of the {@link AbstractSocketImpl} interface for
 * establishing a connection to an ngrok tunnel.
 */
public class ListenerSocketImpl extends AbstractSocketImpl {
    private final Listener listener;

    /**
     * Creates a new tunnel socket implementation for the given tunnel.
     *
     * @param tunnel the tunnel to establish a connection to
     */
    public ListenerSocketImpl(Listener listener) {
        this.listener = listener;
    }

    /**
     * Accepts a tunnel connection to the socket.
     *
     * @param s the socket to accept the connection on
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void accept(SocketImpl s) throws IOException {
        var csi = (ConnectionSocketImpl) s;
        csi.setConnection(listener.accept());
    }
}