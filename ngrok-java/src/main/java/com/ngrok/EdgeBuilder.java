package com.ngrok;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A builder for a {@link EdgeBuilder}.
 */
public class EdgeBuilder extends MetadataBuilder<EdgeBuilder>
        implements Listener.Builder<Listener.Edge>, Forwarder.Builder<Forwarder.Edge> {
    private final Session session;

    private final Map<String, String> labels = new HashMap<>();

    public EdgeBuilder(Session session) {
        this.session = session;
    }

    /**
     * Adds a label with the specified key and value to this builder.
     *
     * @param key the key of the label
     * @param value the value of the label
     * @return the builder instance
     */
    public EdgeBuilder label(String key, String value) {
        labels.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
        return this;
    }

    /**
     * Generates and returns a list of unique labels for this builder.
     *
     * @return a list of unique labels for this builder
     */
    public Map<String, String> getLabels() {
        return Collections.unmodifiableMap(labels);
    }

    @Override
    public Listener.Edge listen() throws IOException {
        return session.listenEdge(this);
    }

    @Override
    public Forwarder.Edge forward(URL url) throws IOException {
        return session.forwardEdge(this, url);
    }
}
