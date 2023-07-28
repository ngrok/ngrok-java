package com.ngrok;

import java.util.Objects;

/**
 * Represents a TCP tunnel with the ngrok service.
 * 
 * {@link AgentTunnel}
 */
public abstract class TcpTunnel extends AgentTunnel {
    /**
     * Constructs a new {@link TcpTunnel} instance with the specified ID, forwarding
     * address, metadata, protocol, and URL.
     *
     * @param id         the ID of the tunnel
     * @param forwardsTo the forwarding address of the tunnel
     * @param metadata   the metadata of the tunnel
     * @param proto      the protocol of the tunnel
     * @param url        the URL of the tunnel
     */
    public TcpTunnel(String id, String forwardsTo, String metadata, String proto, String url) {
        super(id, forwardsTo, metadata, proto, url);
    }

    /**
     * A builder for a {@link TcpTunnel}.
     */
    public static class Builder extends AgentTunnel.Builder<Builder> {
        private String remoteAddress;

        /**
         * Sets the remote address of the tunnel.
         *
         * @param remoteAddress the remote address of the tunnel
         * @return the builder instance
         */
        public Builder remoteAddress(String remoteAddress) {
            this.remoteAddress = Objects.requireNonNull(remoteAddress);
            return this;
        }

        /**
         * Returns whether the builder has a remote address.
         *
         * @return true if the builder has a remote address, false otherwise
         */
        public boolean hasRemoteAddress() {
            return remoteAddress != null;
        }

        /**
         * Returns the remote address of the tunnel.
         *
         * @return the remote address of the tunnel
         */
        public String getRemoteAddress() {
            return remoteAddress;
        }
    }
}