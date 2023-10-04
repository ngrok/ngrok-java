package com.ngrok;

import java.io.IOException;

/**
 * Listener enables applications to handle incoming traffic proxied by ngrok. Each
 * connection to ngrok is forwarded to an instance of a {@link Listener} for processing
 * the implementation specific logic.
 *
 * @param <C> the type of {@link Connection}s this listener accepts
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
     * Represents an endpoint {@link Listener}. This is a listener that is configured
     * by the application on demand, as it is starting to listen.
     */
    interface Endpoint extends Listener<Connection.Endpoint>, ListenerInfo.Endpoint {}

    /**
     * Represents an edge {@link Listener}. This is a listener that is statically
     * configured in ngrok either through the Dashboard or via the API.
     */
    interface Edge extends Listener<Connection.Edge>, ListenerInfo.Edge {}
}
