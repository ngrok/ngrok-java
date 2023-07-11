package com.ngrok.net;

import com.ngrok.Tunnel;

import java.io.IOException;
import java.net.SocketImpl;

public class TunnelSocketImpl extends AbstractSocketImpl {
    private final Tunnel tunnel;

    public TunnelSocketImpl(Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        var csi = (ConnectionSocketImpl) s;
        csi.setConnection(tunnel.accept());
    }
}
