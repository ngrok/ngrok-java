package com.ngrok;

import java.io.IOException;

public interface Forwarder<T extends TunnelInfo> {
    T info();

    void join() throws IOException;

    void close() throws IOException;
}
