package com.ngrok;

import java.io.IOException;

public class NativeLabeledTunnel extends LabeledTunnel {
    private long native_address;

    public NativeLabeledTunnel(String id, String forwardsTo, String metadata) {
        super(id, forwardsTo, metadata);
    }

    @Override
    public native NativeConnection accept() throws IOException;

    @Override
    public native void forwardTcp(String addr) throws IOException;

    @Override
    public native void close() throws IOException;
}
