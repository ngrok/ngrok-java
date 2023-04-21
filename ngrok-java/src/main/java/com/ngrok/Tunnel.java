package com.ngrok;

import java.io.IOException;

public abstract class Tunnel implements AutoCloseable {
    private final String id;
    private final String forwardsTo;
    private final String metadata;

    public Tunnel(String id, String forwardsTo, String metadata) {
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

    public abstract Connection accept() throws IOException;

    public abstract void forwardTcp(String addr) throws IOException;

    public abstract void close() throws IOException;

    public static abstract class Builder<T extends Builder> {
        private String metadata;

        public T metadata(String metadata) {
            this.metadata = metadata;
            return (T) this;
        }

        public boolean hasMetadata() {
            return metadata != null;
        }

        public String getMetadata() {
            return metadata;
        }
    }
}
