package com.ngrok.net;

import com.ngrok.Listener;

import java.io.IOException;
import java.net.SocketImpl;

/**
 * An implementation of the {@link AbstractSocketImpl} interface for
 * accepting connections on a {@link Listener}
 */
public class ListenerSocketImpl extends AbstractSocketImpl {
    private final Listener listener;

    /**
     * Creates a new listener socket implementation for the given listener.
     *
     * @param listener the listener
     */
    public ListenerSocketImpl(Listener listener) {
        this.listener = listener;
    }

    /**
     * Accepts a listener connection to the socket.
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