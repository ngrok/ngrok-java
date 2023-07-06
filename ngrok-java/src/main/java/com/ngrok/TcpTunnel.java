package com.ngrok;

import java.util.Objects;

public abstract class TcpTunnel extends AgentTunnel {
    public TcpTunnel(String id, String forwardsTo, String metadata, String proto, String url) {
        super(id, forwardsTo, metadata, proto, url);
    }

    public static class Builder extends AgentTunnel.Builder<Builder> {
        private String remoteAddress;

        public Builder remoteAddress(String remoteAddress) {
            this.remoteAddress = Objects.requireNonNull(remoteAddress);
            return this;
        }

        public boolean hasRemoteAddress() {
            return remoteAddress != null;
        }

        public String getRemoteAddress() {
            return remoteAddress;
        }
    }
}
