package com.ngrok;

import java.util.HashMap;
import java.util.Map;

public abstract class LabeledTunnel extends Tunnel {

    public static class Builder extends AgentTunnel.Builder {
        public final Map<String, String> labels = new HashMap<>();

        public Builder label(String key, String value) {
            labels.put(key, value);
            return this;
        }

        private String[][] nativeLabels() {
            var strs = new String[this.labels.size()][2];
            int pos = 0;
            for (var e : labels.entrySet()) {
                strs[pos++] = new String[]{e.getKey(), e.getValue()};
            }
            return strs;
        }
    }
}
