package com.ngrok;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ForwardTest {
//    @Test
    public void testForward() throws Exception {
        var session = Session.withAuthtokenFromEnv().connect();
        assertNotNull(session);

        var listener = session.httpEndpoint().domain("ngrok-java-test.ngrok.io")
                .forward(new URL("127.0.0.1:8000"));
        assertNotNull(listener);

        Thread.sleep(10000);
        session.closeListener(listener.getId());

        assertTrue(true);
    }
}