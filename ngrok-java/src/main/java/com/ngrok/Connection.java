package com.ngrok;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public abstract class Connection implements AutoCloseable {
    private String remoteAddr;

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public InetSocketAddress inetAddress() {
        var parts = remoteAddr.split(":");
        return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
    }

    public abstract int read(ByteBuffer dst) throws IOException;

    public abstract int write(ByteBuffer src) throws IOException;

    public abstract void close() throws IOException;
}
