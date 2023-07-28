package com.ngrok;

import java.io.IOException;

/**
 * Base class for all types of ngrok tunnels. Includes common attributes and functionality shared by all types of tunnels.
 *
 * {@link AgentTunnel}
 */
public abstract class Tunnel implements AutoCloseable {
    private final String id;
    private final String forwardsTo;
    private final String metadata;

    /**
     * Constructs a new {@link Tunnel} instance with the specified ID, forwarding address,
     * and metadata.
     *
     * @param id         the ID of the tunnel
     * @param forwardsTo the forwarding address of the tunnel
     * @param metadata   the metadata of the tunnel
     */
    public Tunnel(String id, String forwardsTo, String metadata) {
        this.id = id;
        this.forwardsTo = forwardsTo;
        this.metadata = metadata;
    }

    /**
     * Returns the ID of the tunnel.
     *
     * @return the ID of the tunnel
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the forwarding address of the tunnel.
     *
     * @return the forwarding address of the tunnel
     */
    public String getForwardsTo() {
        return forwardsTo;
    }

    /**
     * Returns the metadata of the tunnel.
     *
     * @return the metadata of the tunnel
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * Accepts a connection on the tunnel.
     *
     * @return the connection accepted on the tunnel
     * @throws IOException if an I/O error occurs while accepting the connection
     */
    public abstract Connection accept() throws IOException;

    /**
     * Forwards TCP traffic to the specified address on the tunnel.
     *
     * @param addr the address to forward TCP traffic to
     * @throws IOException if an I/O error occurs while forwarding TCP traffic
     */
    public abstract void forwardTcp(String addr) throws IOException;

    /**
     * Closes the tunnel.
     *
     * @throws IOException if an I/O error occurs while closing the tunnel
     */
    public abstract void close() throws IOException;

    /**
     * The `Builder` class represents a builder for a tunnel.
     *
     * @param <T> the type of the builder
     */
    public static abstract class Builder<T extends Builder> {
        private String metadata;

        /**
         * Sets the metadata of the tunnel.
         *
         * @param metadata the metadata of the tunnel
         * @return the builder instance
         */
        public T metadata(String metadata) {
            this.metadata = metadata;
            return (T) this;
        }

        /**
         * Returns whether the builder has metadata.
         *
         * @return true if the builder has metadata, false otherwise
         */
        public boolean hasMetadata() {
            return metadata != null;
        }

        /**
         * Returns the metadata of the tunnel.
         *
         * @return the metadata of the tunnel
         */
        public String getMetadata() {
            return metadata;
        }
    }
}