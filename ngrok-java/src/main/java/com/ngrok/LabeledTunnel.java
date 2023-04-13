package com.ngrok;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class LabeledTunnel extends Tunnel {

    public static class Label {
        public final String name;
        public final String value;

        public Label(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String name() {
            return name;
        }

        public String value() {
            return value;
        }
    }

    public static class Builder extends AgentTunnel.Builder {
        public final Map<String, String> labels = new HashMap<>();

        public Builder label(String key, String value) {
            labels.put(key, value);
            return this;
        }

        protected List<Label> labels() {
            return this.labels.entrySet().stream()
                    .map((e) -> new Label(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }
    }
}
