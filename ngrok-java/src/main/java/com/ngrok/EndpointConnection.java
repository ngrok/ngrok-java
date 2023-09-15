package com.ngrok;

public abstract class EndpointConnection extends Connection {
    private final String proto;

    public EndpointConnection(String remoteAddr, String proto) {
        super(remoteAddr);
        this.proto = proto;
    }
    
    public String getProto() {
        return proto;
    }
}
