package com.ngrok;

import java.util.ArrayList;
import java.util.List;

public abstract class AgentTunnel extends Tunnel {
    private final String proto;
    private final String url;

    public AgentTunnel(String id, String forwardsTo, String metadata, String proto, String url) {
        super(id, forwardsTo, metadata);
        this.proto = proto;
        this.url = url;
    }

    public String getProto() {
        return proto;
    }

    /**
     * Retrieves the URL.
     *
     * @return the URL
     */
    public String getUrl() {
        return url;
    }

    public static abstract class Builder<T extends Builder> extends Tunnel.Builder<T> {
        private final List<String> allowCIDR = new ArrayList<>();
        private final List<String> denyCIDR = new ArrayList<>();
        private ProxyProto proxyProto;
        private String forwardsTo;

        public T allowCIDR(String allowCIDR) {
            this.allowCIDR.add(allowCIDR);
            return (T) this;
        }

        public List<String> getAllowCIDR() {
            return allowCIDR;
        }

        public T denyCIDR(String denyCIDR) {
            this.denyCIDR.add(denyCIDR);
            return (T) this;
        }

        public List<String> getDenyCIDR() {
            return denyCIDR;
        }

        public T proxyProto(ProxyProto proxyProto) {
            this.proxyProto = proxyProto;
            return (T) this;
        }

        public boolean hasProxyProto() {
            return proxyProto != null && proxyProto != ProxyProto.None;
        }

        public long getProxyProtoVersion() {
            return proxyProto == null ? 0 : proxyProto.version;
        }

        public T forwardsTo(String forwardsTo) {
            this.forwardsTo = forwardsTo;
            return (T) this;
        }

        public boolean hasForwardsTo() {
            return forwardsTo != null;
        }

        public String getForwardsTo() {
            return forwardsTo;
        }
    }
}
