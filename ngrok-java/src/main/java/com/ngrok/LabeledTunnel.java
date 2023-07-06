package com.ngrok;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class LabeledTunnel extends Tunnel {
    public LabeledTunnel(String id, String forwardsTo, String metadata) {
        super(id, forwardsTo, metadata);
    }

    public static class Label {
        private final String name;
        private final String value;

        public Label(String name, String value) {
            this.name = Objects.requireNonNull(name);
            this.value = Objects.requireNonNull(value);
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    public static class Builder extends Tunnel.Builder<Builder> {
        private final Map<String, String> labels = new HashMap<>();

        public Builder label(String key, String value) {
            labels.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
            return this;
        }

        public List<Label> getLabels() {
            return this.labels.entrySet().stream()
                    .map((e) -> new Label(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }
    }
}
