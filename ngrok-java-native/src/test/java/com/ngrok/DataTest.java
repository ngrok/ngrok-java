package com.ngrok;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class DataTest {

    @Before
    public void setup() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
    }

    @Test
    public void testSessionClose() throws Exception {
        try (var session = Session.connect(Session.newBuilder().metadata("java-session"))) {
            assertEquals("java-session", session.getMetadata());
        }
    }

    @Test
    public void testTunnelClose() throws Exception {
        try (var session = Session.connect(Session.newBuilder());
            var tunnel = session.httpTunnel(new HttpTunnel.Builder().metadata("java-tunnel"))) {
            assertEquals("java-tunnel", tunnel.getMetadata());
            Runtime.getLogger().log("info", "session", tunnel.getUrl());
        }
    }

//    @Test
    public void testPingPong() throws Exception {
        var session = Session.connect(Session.newBuilder());
        assertNotNull(session);

        var tunnel = session.tcpTunnel();
        assertNotNull(tunnel);

        var conn = tunnel.accept();

        var buf = ByteBuffer.allocateDirect(10);
        conn.read(buf);

        System.out.println(buf.asCharBuffer());
        conn.write(buf);

        assertTrue(true);
    }
}
