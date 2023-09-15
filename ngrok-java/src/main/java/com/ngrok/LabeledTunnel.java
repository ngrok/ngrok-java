package com.ngrok;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a labeled tunnel with the ngrok service. 
 * Labeled tunnels are configured by using the <a href="https://ngrok.com/docs/cloud-edge/edges/">edges</a> functionality.
 */
public abstract class LabeledTunnel extends Tunnel<EdgeConnection> {
    /**
     * Constructs a new {@link LabeledTunnel} instance with the specified ID, forwarding address, and metadata.
     *
     * @param id the ID of the tunnel
     * @param forwardsTo the forwarding address of the tunnel
     * @param metadata the metadata of the tunnel
     */
    public LabeledTunnel(String id, String forwardsTo, String metadata) {
        super(id, forwardsTo, metadata);
    }

    /**
     * The {@link Label} class represents a label for a tunnel.
     * Labels are functionally key-value pairs.
     */
    public static class Label {
        private final String name;
        private final String value;

        /**
         * Constructs a new {@link Label} instance with the specified name and value.
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

    /**
     * A builder for a {@link LabeledTunnel}.
     */
    public static class Builder extends Tunnel.Builder<Builder> {
        private final Map<String, String> labels = new HashMap<>();

        /**
         * Adds a label with the specified key and value to this builder.
         *
         * @param key the key of the label
         * @param value the value of the label
         * @return the builder instance
         */
        public Builder label(String key, String value) {
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
    }
}