package com.ngrok;

import java.util.Map;

public class AbstractEdge {
    private final String id;
    private final String metadata;
    private final String forwardsTo;
    private final Map<String, String> labels;

    public AbstractEdge(String id, String metadata, String forwardsTo, Map<String, String> labels) {
        this.id = id;
        this.metadata = metadata;
        this.forwardsTo = forwardsTo;
        this.labels = labels;
    }

    public String getId() {
        return id;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getForwardsTo() {
        return forwardsTo;
    }

    public Map<String, String> getLabels() {
        return labels;
    }
}
