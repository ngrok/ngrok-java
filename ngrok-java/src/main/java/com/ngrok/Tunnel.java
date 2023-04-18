package com.ngrok;

import java.io.IOException;

public abstract class Tunnel implements AutoCloseable {
    private String id;
    private String forwardsTo;
    private String metadata;

    public String id() {
        return id;
    }

    public String forwardsTo() {
        return forwardsTo;
    }

    public String metadata() {
        return metadata;
    }

    public abstract Connection accept() throws IOException;

    public abstract void forwardTcp(String addr) throws IOException;

    public abstract void close() throws IOException;

    public static abstract class Builder<T extends Builder> {
        public String metadata;

        public T metadata(String metadata) {
            this.metadata = metadata;
            return (T) this;
        }
    }
}
