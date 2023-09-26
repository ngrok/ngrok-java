package com.ngrok;

public abstract class TunnelInfo {
    private final String id;
    private final String forwardsTo;
    private final String metadata;

    public TunnelInfo(String id, String forwardsTo, String metadata) {
        this.id = id;
        this.forwardsTo = forwardsTo;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public String getForwardsTo() {
        return forwardsTo;
    }

    public String getMetadata() {
        return metadata;
    }
}
