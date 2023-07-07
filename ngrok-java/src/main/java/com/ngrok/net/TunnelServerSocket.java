package com.ngrok.net;

import com.ngrok.Tunnel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * A server socket for accepting connections to an ngrok tunnel.
 */
public class TunnelServerSocket extends ServerSocket {
    /**
     * Creates a new server socket for the given tunnel.
     *
     * @param tunnel the tunnel to accept connections for
     * @throws IOException if an I/O error occurs
     */
    public TunnelServerSocket(Tunnel tunnel) throws IOException {
        super(new TunnelSocketImpl(tunnel));
    }

    /**
     * Accepts a connection to the server socket.
     *
     * @return A {@link Socket} for the accepted connection
     * @throws IOException if an I/O error occurs
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