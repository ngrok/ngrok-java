package com.ngrok.net;

import com.ngrok.Tunnel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class TunnelServerSocket extends ServerSocket {
    private final int bufferSize;

    public TunnelServerSocket(Tunnel tunnel) {
        this(tunnel, 1024);
    }

    public TunnelServerSocket(Tunnel tunnel, int bufferSize) {
        super(new TunnelSocketImpl(tunnel));
        this.bufferSize = bufferSize;
    }

    public Socket accept() throws IOException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        var s = new ConnectionSocket(bufferSize);
        implAccept(s);
        return s;
    }
}
