package com.ngrok;

/**
 * Enum representing the proxy protocol version.
 */
public enum ProxyProto {
    None(0),
    V1(1),
    V2(2);

    /**
     * The version of the proxy protocol.
     */
    public final long version;

    /**
     * Constructs a new `ProxyProto` instance with the specified version.
     *
     * @param version the version of the proxy protocol
     */
    ProxyProto(int version) {
        this.version = version;
    }

    /**
     * Returns the version of the proxy protocol.
     *
     * @return the version of the proxy protocol
     */
    public long version() {
        return version;
    }
}