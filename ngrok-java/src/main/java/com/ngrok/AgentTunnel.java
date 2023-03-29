package com.ngrok;

public abstract class AgentTunnel extends Tunnel {
    private String proto;
    private String url;

    public String proto() {
        return proto;
    }

    public String url() {
        return url;
    }

    public static abstract class Builder extends Tunnel.Builder {
        public String allowCIDR;
        public String denyCIDR;
        public String forwardsTo;
        // proxyProto

        public <T extends Builder> T allowCIDR(String allowCIDR) {
            this.allowCIDR = allowCIDR;
            return (T) this;
        }

        public <T extends Builder> T denyCIDR(String denyCIDR) {
            this.denyCIDR = denyCIDR;
            return (T) this;
        }

        public <T extends Builder> T forwardsTo(String forwardsTo) {
            this.forwardsTo = forwardsTo;
            return (T) this;
        }
    }
}
