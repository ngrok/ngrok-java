package com.ngrok;

import java.net.URL;
import java.util.Optional;

/**
 * An abstract builder sharing common attributes of all listener builders.
 *
 * @param <T> the concrete builder impl to return to satisfy the builder pattern
 */
public abstract class MetadataBuilder<T extends MetadataBuilder<?>> {
    private Optional<String> metadata = Optional.empty();
    private Optional<String> forwardsTo = Optional.empty();

    /**
     * Sets the metadata for this endpoint.
     *
     * @param metadata the metadata
     * @return An instance the builder represented by type T
     */
    public T metadata(String metadata) {
        this.metadata = Optional.of(metadata);
        return (T) this;
    }

    /**
     * Sets the forwarding information for this endpoint.
     * 
     * If you need to automatically forward connections, you can use {@link Forwarder}, either
     * through using {@link Forwarder.Builder} or directly calling methods on {@link Session}
     * such as {@link Session#forwardHttp(HttpBuilder, URL)}.
     *
     * NOTE: Using the {@link Forwarder} will override what is set here
     * with the actual URL you're forwarding to.
     *
     * @param forwardsTo the forwarding information
     * @return An instance the builder represented by type T
     */
    public T forwardsTo(String forwardsTo) {
        this.forwardsTo = Optional.of(forwardsTo);
        return (T) this;
    }

    /**
     * Returns the metadata for this endpoint.
     *
     * @return the metadata
     */
    public Optional<String> getMetadata() {
        return metadata;
    }

    /**
     * Returns the forwarding information for this endpoint.
     *
     * @return the forwarding information
     */
    public Optional<String> getForwardsTo() {
        return forwardsTo;
    }
}