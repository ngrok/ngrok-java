package com.ngrok.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class ConnectionSocket extends Socket {
    protected ConnectionSocket(int bufferSize) throws IOException {
        super(new ConnectionSocketImpl(bufferSize));
    }

    @Override
    public InetAddress getInetAddress() {
        return super.getInetAddress();
    }
}
