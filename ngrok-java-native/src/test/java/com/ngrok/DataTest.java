package com.ngrok;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class DataTest {

    @Before
    public void setup() {
        System.setProperty("org.slf4j.simpleLogger.log.com.ngrok.Runtime", "trace");
    }

    @Test
    public void testSessionClose() throws Exception {
        try (var session = Session.withAuthtokenFromEnv().metadata("java-session").connect()) {
            assertEquals("java-session", session.getMetadata());
        }
    }

    @Test
    public void testTunnelClose() throws Exception {
        try (var session = Session.withAuthtokenFromEnv().connect();
             var listener = session.httpEndpoint().metadata("java-tunnel").listen()) {
            assertEquals("java-tunnel", listener.getMetadata());
            Runtime.getLogger().log("info", "session", listener.getUrl());
        }
    }

//    @Test
    public void testPingPong() throws Exception {
        var session = Session.withAuthtokenFromEnv().connect();
        assertNotNull(session);

        var tunnel = session.tcpEndpoint().listen();
        assertNotNull(tunnel);

        var conn = tunnel.accept();

        var buf = ByteBuffer.allocateDirect(10);
        conn.read(buf);

        System.out.println(buf.asCharBuffer());
        conn.write(buf);

        assertTrue(true);
    }
}
