package com.ngrok;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * A builder for creating a TCP endpoint listener
 */
public class TcpBuilder extends EndpointBuilder<TcpBuilder>
        implements Listener.Builder<Listener.Endpoint>, Forwarder.Builder<Forwarder.Endpoint> {

    private final Session session;

    private Optional<String> remoteAddress = Optional.empty();

    /**
     * Creates a new {@link TcpBuilder} with a given session.
     *
     * @param session the session over which this listener will connect.
     *                If {@code null}, {@link #listen()} and {@link #forward(URL)}
     *                will throw {@link NullPointerException}, use the corresponding
     *                methods on the {@link Session} object directly.
     */
    public TcpBuilder(Session session) {
        this.session = session;
    }

    /**
     * Sets the TCP address to request for this TCP endpoint.
     *
     * @param remoteAddress the remote address
     * @return the builder instance
     */
    public TcpBuilder remoteAddress(String remoteAddress) {
        this.remoteAddress = Optional.of(remoteAddress);
        return this;
    }

    /**
     * Returns the remote address on this builder.
     *
     * @return the remote address
     */
    public Optional<String> getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public Listener.Endpoint listen() throws IOException {
        return session.listenTcp(this);
    }

    @Override
    public Forwarder.Endpoint forward(URL url) throws IOException {
        return session.forwardTcp(this, url);
    }
}
