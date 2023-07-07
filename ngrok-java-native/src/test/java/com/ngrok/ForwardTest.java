package com.ngrok;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ForwardTest {
    // @Test
    public void testForward() throws Exception {
        var session = Session.connect(Session.newBuilder());
        assertNotNull(session);

        var tunnel = session.httpTunnel(new HttpTunnel.Builder().domain("ngrok-java-test.ngrok.io"));
        assertNotNull(tunnel);

        tunnel.forwardTcp("127.0.0.1:8000");
        assertTrue(true);
    }
}