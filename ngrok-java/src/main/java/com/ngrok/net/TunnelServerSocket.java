package com.ngrok.net;

import com.ngrok.Tunnel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class TunnelServerSocket extends ServerSocket {
    public TunnelServerSocket(Tunnel tunnel) {
        super(new TunnelSocketImpl(tunnel));
    }

    public Socket accept() throws IOException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        var s = new ConnectionSocket();
        implAccept(s);
        return s;
    }
}
