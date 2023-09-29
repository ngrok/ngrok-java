package com.ngrok;

import java.io.IOException;
import java.net.URL;

/**
 * Forwarder is a type of listener which automatically forwards the
 * incoming {@link Connection}s to another url
 */
public interface Forwarder extends ListenerInfo, AutoCloseable {
    /**
     * Waits for this forwarder to complete. After join returns, this
     * forwarder is considered closed.
     *
     * @throws IOException if an I/O error occurs
     */
    void join() throws IOException;

    /**
     * Closes this {@link Forwarder}.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    void close() throws IOException;

    /**
     * Represents a builder that can create new {@link Forwarder} instances.
     *
     * @param <F> the concrete type for the forwarder.
     */
    interface Builder<F extends Forwarder> {
        F forward(URL url) throws IOException;
    }

    /**
     * Represents an endpoint {@link Forwarder}
     */
    interface Endpoint extends Forwarder, ListenerInfo.Endpoint {
    }

    /**
     * Represents an edge {@link Forwarder}
     */
    interface Edge extends Forwarder, ListenerInfo.Edge {
    }
}
