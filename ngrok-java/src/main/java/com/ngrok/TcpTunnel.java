package com.ngrok;

public abstract class TcpTunnel extends AgentTunnel {
    public static class Builder extends AgentTunnel.Builder {
        public String remoteAddress;

        public Builder remoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }
    }
}
