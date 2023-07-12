package com.ngrok;

import java.util.Objects;

/**
 * Uses an agent-aware tunnel to create a TLS connection to ngrok.
 * 
 * @see {@link AgentTunnel}
 */
public abstract class TlsTunnel extends AgentTunnel {
    /**
     * Constructs a new {@link TlsTunnel} instance with the specified ID, forwarding
     * address, metadata, protocol, and URL.
     *
     * @param id         the ID of the tunnel
     * @param forwardsTo the forwarding address of the tunnel
     * @param metadata   the metadata of the tunnel
     * @param proto      the protocol of the tunnel
     * @param url        the URL of the tunnel
     */
    public TlsTunnel(String id, String forwardsTo, String metadata, String proto, String url) {
        super(id, forwardsTo, metadata, proto, url);
    }

    /**
     * A builder for a {@link TlsTunnel}.
     */
    public static class Builder extends AgentTunnel.Builder<Builder> {
        private String domain;

        private byte[] mutualTLSCA;

        private byte[] terminationCertPEM;
        private byte[] terminationKeyPEM;

        /**
         * Sets the domain of the tunnel.
         *
         * @param domain the domain of the tunnel
         * @return the builder instance
         */
        public Builder domain(String domain) {
            this.domain = Objects.requireNonNull(domain);
            return this;
        }

        /**
         * Sets the mutual TLS CA of the tunnel.
         *
         * @param mutualTLSCA the mutual TLS CA of the tunnel
         * @return the builder instance
         */
        public Builder mutualTLSCA(byte[] mutualTLSCA) {
            this.mutualTLSCA = Objects.requireNonNull(mutualTLSCA);
            return this;
        }

        /**
         * Sets the termination certificate and key of the tunnel.
         *
         * @param terminationCertPEM the termination certificate of the tunnel
         * @param terminationKeyPEM  the termination key of the tunnel
         * @return the builder instance
         */
        public Builder termination(byte[] terminationCertPEM, byte[] terminationKeyPEM) {
            this.terminationCertPEM = Objects.requireNonNull(terminationCertPEM);
            this.terminationKeyPEM = Objects.requireNonNull(terminationKeyPEM);
            return this;
        }

        /**
         * Returns the mutual TLS CA of the tunnel.
         *
         * @return the mutual TLS CA of the tunnel
         */
        public byte[] getMutualTLSCA() {
            return mutualTLSCA;
        }

        /**
         * Returns the termination certificate in PEM format.
         *
         * @return the termination certificate in PEM format
         */
        public byte[] getTerminationCertPEM() {
            return terminationCertPEM;
        }

        /**
         * Returns the termination key in PEM format.
         *
         * @return the termination key in PEM format
         */
        public byte[] getTerminationKeyPEM() {
            return terminationKeyPEM;
        }

        /**
         * Checks if the domain is not null.
         *
         * @return true if the domain is not null, false otherwise
         */
        public boolean hasDomain() {
            return domain != null;
        }

        /**
         * Retrieves the domain.
         *
         * @return the domain
         */
        public String getDomain() {
            return domain;
        }
    }
}
