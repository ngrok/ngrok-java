package com.ngrok;

import java.io.IOException;
import java.util.Map;

public interface Listener<C extends Connection> extends AutoCloseable {
    String getId();

    String getMetadata();

    String getForwardsTo();

    C accept() throws IOException;

    @Override
    void close() throws IOException;

    interface Builder<T extends Listener> {
        T listen() throws IOException;
    }

    interface Endpoint extends Listener<Connection.Endpoint> {
        String getProto();

        String getUrl();
    }

    interface Edge extends Listener<Connection.Edge> {
        Map<String, String> getLabels();
    }
}
