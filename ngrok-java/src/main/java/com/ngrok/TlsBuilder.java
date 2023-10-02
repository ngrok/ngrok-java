package com.ngrok;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

/**
 * A builder for creating a TLS endpoint listener
 */
public class TlsBuilder extends EndpointBuilder<TlsBuilder>
        implements Listener.Builder<Listener.Endpoint>, Forwarder.Builder<Forwarder.Endpoint> {
    private final Session session;

    private Optional<String> domain = Optional.empty();

    private byte[] mutualTLSCA;

    private byte[] terminationCertPEM;
    private byte[] terminationKeyPEM;

    /**
     * Creates a new {@link TlsBuilder} with a given session.
     *
     * @param session the session over which this listener will connect.
     *                If {@code null}, {@link #listen()} and {@link #forward(URL)}
     *                will throw {@link NullPointerException}, use the corresponding
     *                methods on the {@link Session} object directly.
     */
    public TlsBuilder(Session session) {
        this.session = session;
    }

    /**
     * Set the domain for this builder.
     *
     * @param domain the domain
     * @return the builder instance
     */
    public TlsBuilder domain(String domain) {
        this.domain = Optional.of(domain);
        return this;
    }

    /**
     * Set the mutual TLS certificate authority for this builder.
     *
     * @param mutualTLSCA the TLS certificate authority, in bytes
     * @return the builder instance
     */
    public TlsBuilder mutualTLSCA(byte[] mutualTLSCA) {
        this.mutualTLSCA = Objects.requireNonNull(mutualTLSCA);
        return this;
    }

    /**
     *  Set TLS termination for this builder.
     *
     * @param terminationCertPEM the TLS certificate, in bytes
     * @param terminationKeyPEM the TLS key, in bytes
     * @return the builder instance
     */
    public TlsBuilder termination(byte[] terminationCertPEM, byte[] terminationKeyPEM) {
        this.terminationCertPEM = Objects.requireNonNull(terminationCertPEM);
        this.terminationKeyPEM = Objects.requireNonNull(terminationKeyPEM);
        return this;
    }

    /**
     * Returns the domain on this builder.
     *
     * @return the domain
     */
    public Optional<String> getDomain() {
        return domain;
    }

    /**
     * Returns the mutual TLS certificate authority on this builder.
     *
     * @return the TLS certificate authority, in bytes.
     */
    public byte[] getMutualTLSCA() {
        return mutualTLSCA;
    }

    /**
     * Returns the TLS termination certificate PEM on this builder.
     *
     * @return the TLS termination certificate, in bytes.
     */
    public byte[] getTerminationCertPEM() {
        return terminationCertPEM;
    }

    /**
     * Returns the TLS termination key PEM on this builder.
     *
     * @return the TLS termination key, in bytes.
     */
    public byte[] getTerminationKeyPEM() {
        return terminationKeyPEM;
    }

    @Override
    public Listener.Endpoint listen() throws IOException {
        return session.listenTls(this);
    }

    @Override
    public Forwarder.Endpoint forward(URL url) throws IOException {
        return session.forwardTls(this, url);
    }
}