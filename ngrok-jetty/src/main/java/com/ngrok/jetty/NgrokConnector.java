package com.ngrok.jetty;

import com.ngrok.Connection;
import com.ngrok.Session;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A class representing a connector implementation for ngrok listeners.
 */
public class NgrokConnector<C extends Connection> extends AbstractConnector {
    private final Supplier<Session> sessionSupplier;
    private final Function<Session, com.ngrok.Listener<C>> listenerFunction;
    private Session session;
    private com.ngrok.Listener<C> listener;

    /**
     * Constructs a new ngrok connector with the specified server, session supplier,
     * and listener function.
     *
     * @param server           the server to use for the connector
     * @param sessionSupplier  the supplier for the session used by the connector
     * @param listenerFunction the function for creating the listener
     */
    public NgrokConnector(Server server, Supplier<Session> sessionSupplier, Function<Session, com.ngrok.Listener<C>> listenerFunction) {
        super(server, null, null, null, -1, new HttpConnectionFactory());
        setDefaultProtocol(HttpVersion.HTTP_1_1.asString());

        this.sessionSupplier = sessionSupplier;
        this.listenerFunction = listenerFunction;
    }

    /**
     * Starts this ngrok connector.
     *
     * @throws Exception if an error occurs while starting the connector
     */
    @Override
    protected void doStart() throws Exception {
        this.session = sessionSupplier.get();
        this.listener = listenerFunction.apply(this.session);
        super.doStart();
    }

    /**
     * Accepts a new connection on this ngrok connector.
     *
     * @param i the ID of the connection
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    @Override
    protected void accept(int i) throws IOException, InterruptedException {
        var ngrokConnection = listener.accept();
        var ep = new NgrokEndpoint(getScheduler(), ngrokConnection);

        var connection = getDefaultConnectionFactory().newConnection(this, ep);
        ep.setConnection(connection);

        connection.onOpen();
    }

    /**
     * Stops this ngrok connector.
     *
     * @throws Exception if an error occurs while stopping the connector
     */
    @Override
    protected void doStop() throws Exception {
        super.doStop();
        this.listener.close();
    }

    /**
     * Throws an {@link UnsupportedOperationException}, as the transport used by ngrok
     * connector is not supported.
     *
     * @throws UnsupportedOperationException if the method is called
     */
    @Override
    public Object getTransport() {
        throw new UnsupportedOperationException("ohnoe");
    }
}
