package com.ngrok.jetty;

import com.ngrok.AgentTunnel;
import com.ngrok.Session;

import com.ngrok.Tunnel;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

public class NgrokConnector extends AbstractConnector {
    private final Supplier<Session> sessionSupplier;
    private final Function<Session, Tunnel> tunnelFunction;

    private Session session;
    private Tunnel tunnel;

    public NgrokConnector(Server server, Supplier<Session> sessionSupplier, Function<Session, Tunnel> tunnelFunction) {
        super(server, null, null, null, -1, new HttpConnectionFactory());
        setDefaultProtocol(HttpVersion.HTTP_1_1.asString());

        this.sessionSupplier = sessionSupplier;
        this.tunnelFunction = tunnelFunction;
    }

    @Override
    protected void doStart() throws Exception {
        this.session = sessionSupplier.get();
        this.tunnel = tunnelFunction.apply(this.session);
        if (this.tunnel instanceof AgentTunnel agentTunnel) {
            System.out.printf("URL: %s\n", agentTunnel.url());
        }

        super.doStart();
    }

    @Override
    protected void accept(int i) throws IOException, InterruptedException {
        var nconn = tunnel.accept();
        System.out.printf("[%s] Accepted for %d\n", nconn.remoteAddr(), i);
        var ep = new NgrokEndpoint(getScheduler(), nconn);

        var conn = getDefaultConnectionFactory().newConnection(this, ep);
        ep.setConnection(conn);

        conn.onOpen();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        this.tunnel.close();
    }

    @Override
    public Object getTransport() {
        throw new UnsupportedOperationException("ohnoe");
    }
}
