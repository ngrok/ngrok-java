package com.ngrok;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public interface Forwarder extends ListenerInfo, AutoCloseable {
    void join() throws IOException;

    @Override
    void close() throws IOException;

    interface Builder<F extends Forwarder> {
        F forward(URL url) throws IOException;
    }

    interface Endpoint extends Forwarder, ListenerInfo.Endpoint {
    }

    interface Edge extends Forwarder, ListenerInfo.Edge {
    }
}
