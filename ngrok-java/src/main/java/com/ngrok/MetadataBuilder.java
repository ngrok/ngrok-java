package com.ngrok;

import java.util.Optional;

/**
 * An abstract builder sharing common attributes of all listener builders.
 *
 * @param <T> the concrete builder impl to return to satisfy the builder pattern
 */
public abstract class MetadataBuilder<T extends MetadataBuilder> {
    private Optional<String> metadata = Optional.empty();
    private Optional<String> forwardsTo = Optional.empty();

    /**
     * Sets the metadata for this builder.
     *
     * @param metadata the metadata
     * @return An instance the builder represented by type T
     */
    public T metadata(String metadata) {
        this.metadata = Optional.of(metadata);
        return (T) this;
    }

    /**
     * Sets the forwarding address for this builder.
     *
     * @param forwardsTo the forwarding address for the builder
     * @return An instance the builder represented by type T
     */
    public T forwardsTo(String forwardsTo) {
        this.forwardsTo = Optional.of(forwardsTo);
        return (T) this;
    }

    /**
     * Returns the metadata for this builder.
     *
     * @return the metadata
     */
    public Optional<String> getMetadata() {
        return metadata;
    }


    /**
     * Returns the forwarding address for this builder.
     *
     * @return the currently set forwarding address
     */
    public Optional<String> getForwardsTo() {
        return forwardsTo;
    }
}