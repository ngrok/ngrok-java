package com.ngrok;

import java.io.IOException;

public class NativeTcpTunnel extends TcpTunnel {
    private long native_address;

    @Override
    public native NativeConnection accept() throws IOException;

    @Override
    public native void forwardTcp(String addr);

    @Override
    public native void close() throws IOException;
}