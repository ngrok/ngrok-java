package com.ngrok;

import java.io.IOException;

/**
 *
 * @param <C>
 */
public interface Listener<C extends Connection> extends ListenerInfo, AutoCloseable {
    /**
     * Waits for the next connection and returns it.
     *
     * @return the connection
     * @throws IOException if an I/O error occurs
     */
    C accept() throws IOException;

    /**
     * Closes this {@link Listener}.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    void close() throws IOException;

    /**
     * Represents a builder that can create new {@link Listener} instances.
     *
     * @param <L> the concrete type for the listener.
     */
    interface Builder<L extends Listener> {
        /**
         * Starts listening and accepting new connections.
         *
         * @return the concrete {@link Listener} instance
         * @throws IOException if an I/O error occurs
         */
        L listen() throws IOException;
    }

    /**
     * Represents an endpoint {@link Listener}
     */
    interface Endpoint extends Listener<Connection.Endpoint>, ListenerInfo.Endpoint {}

    /**
     * Represents an edge {@link Listener}
     */
    interface Edge extends Listener<Connection.Edge>, ListenerInfo.Edge {}
}
