package com.ngrok;

public enum ProxyProto {
    None(0),
    V1(1),
    V2(2);

    public final int version;

    ProxyProto(int version) {
        this.version = version;
    }

    public int version() {
        return version;
    }
}
