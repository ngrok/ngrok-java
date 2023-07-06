package com.ngrok;

import java.util.Objects;

public abstract class TlsTunnel extends AgentTunnel {
    public TlsTunnel(String id, String forwardsTo, String metadata, String proto, String url) {
        super(id, forwardsTo, metadata, proto, url);
    }

    public static class Builder extends AgentTunnel.Builder<Builder> {
        private String domain;

        private byte[] mutualTLSCA;

        private byte[] terminationCertPEM;
        private byte[] terminationKeyPEM;

        public Builder domain(String domain) {
            this.domain = Objects.requireNonNull(domain);
            return this;
        }

        public Builder mutualTLSCA(byte[] mutualTLSCA) {
            this.mutualTLSCA = Objects.requireNonNull(mutualTLSCA);
            return this;
        }

        public Builder termination(byte[] terminationCertPEM, byte[] terminationKeyPEM) {
            this.terminationCertPEM = Objects.requireNonNull(terminationCertPEM);
            this.terminationKeyPEM = Objects.requireNonNull(terminationKeyPEM);
            return this;
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

        public boolean hasDomain() {
            return domain != null;
        }

        public String getDomain() {
            return domain;
        }
    }
}
