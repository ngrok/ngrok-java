package com.ngrok;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
public class TlsBuilder extends EndpointBuilder<TlsBuilder>
        implements Listener.Builder<Listener.Endpoint>, Forwarder.Builder<Forwarder.Endpoint> {
    private final Session session;

    private Optional<String> domain;

    private byte[] mutualTLSCA;

    private byte[] terminationCertPEM;
    private byte[] terminationKeyPEM;

    public TlsBuilder(Session session) {
        this.session = session;
    }

    public TlsBuilder domain(String domain) {
        this.domain = Optional.of(domain);
        return this;
    }

    public TlsBuilder mutualTLSCA(byte[] mutualTLSCA) {
        this.mutualTLSCA = Objects.requireNonNull(mutualTLSCA);
        return this;
    }

    public TlsBuilder termination(byte[] terminationCertPEM, byte[] terminationKeyPEM) {
        this.terminationCertPEM = Objects.requireNonNull(terminationCertPEM);
        this.terminationKeyPEM = Objects.requireNonNull(terminationKeyPEM);
        return this;
    }

    public Optional<String> getDomain() {
        return domain;
    }

    public byte[] getMutualTLSCA() {
        return mutualTLSCA;
    }

    public byte[] getTerminationCertPEM() {
        return terminationCertPEM;
    }

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