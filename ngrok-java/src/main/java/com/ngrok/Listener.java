package com.ngrok;

import java.io.IOException;
import java.util.Map;

/**
 *
 * @param <C>
 */
public interface Listener<C extends Connection> extends ListenerInfo, AutoCloseable {
    C accept() throws IOException;

    @Override
    void close() throws IOException;

    interface Builder<T extends Listener> {
        T listen() throws IOException;
    }

    interface Endpoint extends Listener<Connection.Endpoint>, ListenerInfo.Endpoint {
    }

    interface Edge extends Listener<Connection.Edge>, ListenerInfo.Edge {
    }
}
