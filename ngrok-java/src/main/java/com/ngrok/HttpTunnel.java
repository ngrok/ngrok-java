package com.ngrok;

public abstract class HttpTunnel extends AgentTunnel {
    public static class Builder extends AgentTunnel.Builder<Builder> {
        public String domain;

        public byte[] mutualTLSCA;

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder mutualTLSCA(byte[] mutualTLSCA) {
            this.mutualTLSCA = mutualTLSCA;
            return this;
        }

        public byte[] mutualTLSCA() {
            return mutualTLSCA;
        }
    }
}
