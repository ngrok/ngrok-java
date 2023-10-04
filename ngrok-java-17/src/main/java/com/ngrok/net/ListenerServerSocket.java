package com.ngrok.net;

import com.ngrok.Listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * A server socket for accepting connections from a {@link Listener}.
 */
public class ListenerServerSocket extends ServerSocket {
    /**
     * Creates a new server socket for the given listener.
     *
     * @param listener the listener to accept connections for
     * @throws IOException if an I/O error occurs
     */
    public ListenerServerSocket(Listener listener) throws IOException {
        super(new ListenerSocketImpl(listener));
    }

    /**
     * Accepts a connection to the server socket.
     *
     * @return A {@link Socket} for the accepted connection
     * @throws IOException     if an I/O error occurs
     * @throws SocketException if the socket is closed
     */
    @Override
    public Socket accept() throws IOException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        var s = new ConnectionSocket();
        implAccept(s);
        return s;
    }
}