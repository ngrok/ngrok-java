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
                var listener = session.httpEndpoint().metadata("java-endpoint").listen()) {
            assertEquals("java-endpoint", listener.getMetadata());
            Runtime.getLogger().log("info", "session", listener.getUrl());
        }
    }

    // @Test
    public void testPingPong() throws Exception {
        var session = Session.withAuthtokenFromEnv().connect();
        assertNotNull(session);

        var listener = session.tcpEndpoint().listen();
        assertNotNull(listener);

        var conn = listener.accept();

        var buf = ByteBuffer.allocateDirect(10);
        conn.read(buf);

        System.out.println(buf.asCharBuffer());
        conn.write(buf);

        assertTrue(true);
    }

    @Test
    public void testPolicy() throws Exception {
        final ClassLoader classLoader = getClass().getClassLoader();
        final String policy = new String(classLoader.getResourceAsStream("policy.json").readAllBytes());

        try (var session = Session.withAuthtokenFromEnv().connect();
                var listener = session.httpEndpoint().metadata("java-endpoint").policy(policy).listen()) {
            assertEquals("java-endpoint", listener.getMetadata());
            Runtime.getLogger().log("info", "session", listener.getUrl());
        }
    }
}
