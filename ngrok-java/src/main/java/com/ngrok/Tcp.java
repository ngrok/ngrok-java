package com.ngrok;

import java.util.Objects;

public interface Tcp {
    interface Listener extends com.ngrok.Listener<EndpointInfo, EndpointConnection> {}
    interface Forwarder extends com.ngrok.Forwarder<EndpointInfo> {}

    /**
     * A builder for a {@link Tcp}.
     */
    class Builder extends EndpointTunnel.Builder<Builder> {
        private String remoteAddress;

        /**
         * Sets the remote address of the tunnel.
         *
         * @param remoteAddress the remote address of the tunnel
         * @return the builder instance
         */
        public Tcp.Builder remoteAddress(String remoteAddress) {
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
