package com.ngrok;

import java.io.IOException;

public interface Forwarder<T extends TunnelInfo> extends AutoCloseable {
    T info();

    void join() throws IOException;

    @Override
    void close() throws IOException;
}
