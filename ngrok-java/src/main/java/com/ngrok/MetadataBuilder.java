package com.ngrok;

import java.util.Optional;

/**
 * An abstract builder sharing common attributes of all listener builders.
 *
 * @param <T> the concrete builder impl to return to satisfy the builder pattern
 */
public abstract class MetadataBuilder<T extends MetadataBuilder> {
    private Optional<String> metadata = Optional.empty();

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
     * Returns the metadata for this builder.
     *
     * @return the metadata
     */
    public Optional<String> getMetadata() {
        return metadata;
    }
}
