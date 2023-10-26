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
     * Sets the domain to request for this TLS endpoint. Any valid domain or hostname
     * that you have previously registered with ngrok. If using a custom domain, this requires
     * registering in the <a href="https://dashboard.ngrok.com/cloud-edge/domains">ngrok dashboard</a>
     * and setting a DNS CNAME value.
     *
     * @param domain the domain
     * @return the builder instance
     */
    public TlsBuilder domain(String domain) {
        this.domain = Optional.of(domain);
        return this;
    }

    /**
     * Sets the certificates to use for client authentication for this TLS endpoint.
     *
     * @param mutualTLSCA the TLS certificate, in bytes
     * @return the builder instance
     *
     * @see <a href="https://ngrok.com/docs/tls/mutual-tls/">Mutual TLS</a>
     * in the ngrok docs for additional details.
     */
    public TlsBuilder mutualTLSCA(byte[] mutualTLSCA) {
        this.mutualTLSCA = Objects.requireNonNull(mutualTLSCA);
        return this;
    }

    /**
     * Sets the certificate and key to use for TLS termination for this TLS endpoint.
     *
     * @param terminationCertPEM the TLS certificate, in bytes
     * @param terminationKeyPEM the TLS key, in bytes
     * @return the builder instance
     *
     * @see <a href="https://ngrok.com/docs/tls/tls-termination/">TLS Termination</a>
     * in the ngrok docs for additional details.
     */
    public TlsBuilder termination(byte[] terminationCertPEM, byte[] terminationKeyPEM) {
        this.terminationCertPEM = Objects.requireNonNull(terminationCertPEM);
        this.terminationKeyPEM = Objects.requireNonNull(terminationKeyPEM);
        return this;
    }

    /**
     * Returns the domain to request for this TLS endpoint.
     *
     * @return the domain
     */
    public Optional<String> getDomain() {
        return domain;
    }

    /**
     * Returns the certificates to use for client authentication for this TLS endpoint.
     *
     * @return the TLS certificates, in bytes.
     */
    public byte[] getMutualTLSCA() {
        return mutualTLSCA;
    }

    /**
     * Sets the certificate to use for TLS termination for this TLS endpoint.
     *
     * @return the TLS termination certificate, in bytes.
     */
    public byte[] getTerminationCertPEM() {
        return terminationCertPEM;
    }

    /**
     * Sets the key to use for TLS termination for this TLS endpoint.
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