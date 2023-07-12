package com.ngrok;

import java.util.ArrayList;
import java.util.List;

/**
 * The `AgentTunnel` class is an abstract class that extends the {@link Tunnel}
 * class and represents a tunnel for an agent, with properties such as protocol,
 * URL, and forwarding options.
 * 
 * @see {@link TcpTunnel}
 * @see {@link TlsTunnel}
 */
public abstract class AgentTunnel extends Tunnel {
    private final String proto;
    private final String url;

    /**
     * Creates a new agent tunnel with the given properties.
     *
     * @param id         the ID of the tunnel
     * @param forwardsTo the address to forward traffic to
     * @param metadata   metadata associated with the tunnel
     * @param proto      the protocol used by the tunnel
     * @param url        the URL of the tunnel
     */
    public AgentTunnel(String id, String forwardsTo, String metadata, String proto, String url) {
        super(id, forwardsTo, metadata);
        this.proto = proto;
        this.url = url;
    }

    /**
     * Returns the protocol used by the tunnel.
     *
     * @return the protocol used by the tunnel
     */
    public String getProto() {
        return proto;
    }

    /**
     * Returns the URL of the tunnel.
     *
     * @return the URL of the tunnel
     */
    public String getUrl() {
        return url;
    }

    /**
     * A builder for creating new agent tunnels.
     *
     * @param <T> the type of the builder
     */
    public static abstract class Builder<T extends Builder> extends Tunnel.Builder<T> {
        private final List<String> allowCIDR = new ArrayList<>();
        private final List<String> denyCIDR = new ArrayList<>();
        private ProxyProto proxyProto;
        private String forwardsTo;

        /**
         * The function allows the addition of a CIDR (Classless Inter-Domain Routing)
         * address to a
         * list.
         * 
         * @param allowCIDR The parameter "allowCIDR" is a string that represents a
         *                  Classless
         *                  Inter-Domain Routing (CIDR) notation. It is used to specify
         *                  a range of IP addresses that are
         *                  allowed.
         * @return An instance of the class that the method is defined in, which is
         *         represented by the generic type T.
         */
        public T allowCIDR(String allowCIDR) {
            this.allowCIDR.add(allowCIDR);
            return (T) this;
        }

        /**
         * The function returns a list of strings representing allowed CIDR addresses.
         * 
         * @return A list of strings representing allowed CIDR addresses.
         */
        public List<String> getAllowCIDR() {
            return allowCIDR;
        }

        /**
         * The function adds a denyCIDR to a list.
         * 
         * @param denyCIDR The parameter "denyCIDR" is a string that represents a
         *                 Classless
         *                 Inter-Domain Routing (CIDR) notation. It is used to specify a
         *                 range of IP addresses that
         *                 should be denied access.
         * @return An instancxe of the class that the method is defined in, with the
         *         generic type T.
         */
        public T denyCIDR(String denyCIDR) {
            this.denyCIDR.add(denyCIDR);
            return (T) this;
        }

        /**
         * The function returns a list of strings representing CIDR blocks that should
         * be denied.
         * 
         * @return A list of strings representing denied CIDR addresses.
         */
        public List<String> getDenyCIDR() {
            return denyCIDR;
        }

        /**
         * Sets the proxy protocol for the tunnel.
         *
         * @param proxyProto the proxy protocol for the tunnel
         * @return the tunnel instance
         */
        public T proxyProto(ProxyProto proxyProto) {
            this.proxyProto = proxyProto;
            return (T) this;
        }

        /**
         * Returns whether the tunnel has a proxy protocol.
         *
         * @return true if the tunnel has a proxy protocol, false otherwise
         */
        public boolean hasProxyProto() {
            return proxyProto != null && proxyProto != ProxyProto.None;
        }

        /**
         * Returns the version of the proxy protocol for the tunnel.
         *
         * @return the version of the proxy protocol for the tunnel
         */
        public long getProxyProtoVersion() {
            return proxyProto == null ? 0 : proxyProto.version;
        }

        /**
         * Sets the forwarding address for the tunnel.
         *
         * @param forwardsTo the forwarding address for the tunnel
         * @return the tunnel instance
         */
        public T forwardsTo(String forwardsTo) {
            this.forwardsTo = forwardsTo;
            return (T) this;
        }

        /**
         * Returns whether the tunnel has a forwarding address.
         *
         * @return true if the tunnel has a forwarding address, false otherwise
         */
        public boolean hasForwardsTo() {
            return forwardsTo != null;
        }

        /**
         * Returns the forwarding address for the tunnel.
         *
         * @return the forwarding address for the tunnel
         */
        public String getForwardsTo() {
            return forwardsTo;
        }
    }
}
