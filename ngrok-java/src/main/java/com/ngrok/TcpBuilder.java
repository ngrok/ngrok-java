package com.ngrok;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class TcpBuilder extends EndpointBuilder<TcpBuilder>
        implements Listener.Builder<Listener.Endpoint>, Forwarder.Builder<Forwarder.Endpoint> {

    private final Session session;

    private Optional<String> remoteAddress = Optional.empty();

    public TcpBuilder(Session session) {
        this.session = session;
    }

    public TcpBuilder remoteAddress(String remoteAddress) {
        this.remoteAddress = Optional.of(remoteAddress);
        return this;
    }

    public Optional<String> getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public Listener.Endpoint listen() throws IOException {
        return session.listenTcp(this);
    }


    @Override
    public Forwarder.Endpoint forward(URL url) throws IOException {
        return session.forwardTcp(this, url);
    }
}
