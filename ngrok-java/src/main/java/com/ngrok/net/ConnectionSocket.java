package com.ngrok.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class ConnectionSocket extends Socket {
    protected ConnectionSocket() throws IOException {
        super(new ConnectionSocketImpl());
    }

    @Override
    public InetAddress getInetAddress() {
        return super.getInetAddress();
    }
}
