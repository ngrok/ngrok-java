package com.ngrok;

public abstract class TlsTunnel extends AgentTunnel {
    public static class Builder extends AgentTunnel.Builder<Builder> {
        public String domain;

        public byte[] mutualTLSCA;

        public byte[] terminationCertPEM;
        public byte[] terminationKeyPEM;

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder mutualTLSCA(byte[] mutualTLSCA) {
            this.mutualTLSCA = mutualTLSCA;
            return this;
        }

        public Builder termination(byte[] terminationCertPEM, byte[] terminationKeyPEM) {
            this.terminationCertPEM = terminationCertPEM;
            this.terminationKeyPEM = terminationKeyPEM;
            return this;
        }

        public byte[] mutualTLSCA() {
            return mutualTLSCA;
        }

        public byte[] terminationCertPEM() {
            return terminationCertPEM;
        }

        public byte[] terminationKeyPEM() {
            return terminationKeyPEM;
        }
    }
}
