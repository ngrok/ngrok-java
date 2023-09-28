package com.ngrok;

public abstract class AbstractEndpoint {
    private final String id;
    private final String metadata;
    private final String forwardsTo;
    private final String proto;
    private final String url;

    public AbstractEndpoint(String id, String metadata, String forwardsTo, String proto, String url) {
        this.id = id;
        this.metadata = metadata;
        this.forwardsTo = forwardsTo;
        this.proto = proto;
        this.url = url;
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

    public String getProto() {
        return proto;
    }

    public String getUrl() {
        return url;
    }
}
