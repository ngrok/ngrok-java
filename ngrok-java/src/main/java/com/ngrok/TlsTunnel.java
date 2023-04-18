package com.ngrok;

public abstract class TlsTunnel extends AgentTunnel {
    public static class Builder extends AgentTunnel.Builder<Builder> {
        public String domain;

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }
    }
}
