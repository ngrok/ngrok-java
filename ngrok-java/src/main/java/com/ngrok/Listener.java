package com.ngrok;

import java.io.IOException;

public interface Listener<T extends TunnelInfo, C extends Connection> {
    T info();

    C accept() throws IOException;

    void close() throws IOException;
}
