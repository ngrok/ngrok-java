package com.ngrok;

public abstract class EdgeConnection extends Connection {
    private final String type;
    private final boolean passthroughTls;
    
    public EdgeConnection(String remoteAddr, String type, boolean passthroughTls) {
        super(remoteAddr);
        this.type = type;
        this.passthroughTls = passthroughTls;
    }

    public String getType() {
        return type;
    }

    public boolean isPassthroughTls() {
        return passthroughTls;
    }
}
