package com.ngrok;

public class EndpointInfo extends TunnelInfo {
    private final String proto;
    private final String url;

    public EndpointInfo(String id, String forwardsTo, String metadata, String proto, String url) {
        super(id, forwardsTo, metadata);
        this.proto = proto;
        this.url = url;
    }

    public String getProto() {
        return proto;
    }

    public String getUrl() {
        return url;
    }
}
