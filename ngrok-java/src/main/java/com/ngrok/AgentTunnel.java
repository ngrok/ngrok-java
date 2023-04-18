package com.ngrok;

import java.util.ArrayList;
import java.util.List;

public abstract class AgentTunnel extends Tunnel {
    private String proto;
    private String url;

    public String proto() {
        return proto;
    }

    public String url() {
        return url;
    }

    public static abstract class Builder<T extends Builder> extends Tunnel.Builder<T> {
        public final List<String> allowCIDR = new ArrayList<>();
        public final List<String> denyCIDR = new ArrayList<>();
        public ProxyProto proxyProto;
        public String forwardsTo;

        public T allowCIDR(String allowCIDR) {
            this.allowCIDR.add(allowCIDR);
            return (T) this;
        }

        public T denyCIDR(String denyCIDR) {
            this.denyCIDR.add(denyCIDR);
            return (T) this;
        }

        public T proxyProto(ProxyProto proxyProto) {
            this.proxyProto = proxyProto;
            return (T) this;
        }

        public T forwardsTo(String forwardsTo) {
            this.forwardsTo = forwardsTo;
            return (T) this;
        }
    }
}
