package com.ngrok;

import java.util.*;

public abstract class MetadataBuilder<T extends MetadataBuilder> {
    private Optional<String> metadata = Optional.empty();

    // Chainable methods

    public T metadata(String metadata) {
        this.metadata = Optional.of(metadata);
        return (T) this;
    }

    // Accessors

    public Optional<String> getMetadata() {
        return metadata;
    }
}
