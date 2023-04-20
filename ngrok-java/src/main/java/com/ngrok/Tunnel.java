package com.ngrok;

import java.io.IOException;

public abstract class Tunnel implements AutoCloseable {
    private String id;
    private String forwardsTo;
    private String metadata;

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
