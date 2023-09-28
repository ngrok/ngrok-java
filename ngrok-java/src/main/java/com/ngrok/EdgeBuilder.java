package com.ngrok;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    public List<Label> getLabels() {
        return this.labels.entrySet().stream()
                .map(entry -> new Label(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public Listener.Edge listen() throws IOException {
        return session.listenEdge(this);
    }

    @Override
    public Forwarder.Edge forward(URL url) throws IOException {
        return session.forwardEdge(this, url);
    }

    public static class Label {
        private final String name;
        private final String value;

        /**
         * Constructs a new {@link EdgeBuilder.Label} instance with the specified name and value.
         * Name and value must not be null.
         *
         * @param name the name of the label
         * @param value the value of the label
         */
        public Label(String name, String value) {
            this.name = Objects.requireNonNull(name);
            this.value = Objects.requireNonNull(value);
        }

        /**
         * Returns the name of the label.
         *
         * @return the name of the label
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the value of the label.
         *
         * @return the value of the label
         */
        public String getValue() {
            return value;
        }
    }
}
