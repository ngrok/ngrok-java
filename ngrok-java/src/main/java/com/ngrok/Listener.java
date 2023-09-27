package com.ngrok;

import java.io.IOException;

public interface Listener<T extends TunnelInfo, C extends Connection> extends AutoCloseable {
    T info();

    C accept() throws IOException;

    @Override
    void close() throws IOException;
}
