package com.ngrok;

import org.junit.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.Assert.*;

public class ReconnectTest {
    // connect() checks the reconnect options before it opens a connection.
    // The server address is a local port so that, if that check were lost,
    // the test fails on a refused connection rather than reaching ngrok.
    private static IOException connectRefusal(Duration initial, Duration max) {
        var builder = Session.withAuthtoken("unused")
                .serverAddr("127.0.0.1:1")
                .reconnectBackoff(initial, max);
        return assertThrows(IOException.class, builder::connect);
    }

    @Test
    public void connectRefusesAStartBelowOneMillisecond() {
        var e = connectRefusal(Duration.ZERO, Duration.ofSeconds(1));
        assertEquals("reconnectBackoff: invalid reconnect backoff (start 0ns, cap 1s): "
                + "the start must be at least 1 ms", e.getMessage());
    }

    @Test
    public void connectRefusesAStartJustBelowOneMillisecond() {
        // A nanosecond short of 1 ms: truncating to whole milliseconds would
        // turn this into 0 ms, so the start is shown as it was given.
        var e = connectRefusal(Duration.ofNanos(999_999), Duration.ofSeconds(1));
        assertEquals("reconnectBackoff: invalid reconnect backoff (start 999.999µs, cap 1s): "
                + "the start must be at least 1 ms", e.getMessage());
    }

    @Test
    public void connectRefusesAStartAboveTheCap() {
        var e = connectRefusal(Duration.ofSeconds(2), Duration.ofSeconds(1));
        assertEquals("reconnectBackoff: invalid reconnect backoff (start 2s, cap 1s): "
                + "the start must not be above the cap", e.getMessage());
    }

    @Test
    public void connectRefusesACapJustAboveOneDay() {
        // One nanosecond over: truncating to whole milliseconds would accept it.
        var e = connectRefusal(Duration.ofMillis(1), Duration.ofDays(1).plusNanos(1));
        assertEquals("reconnectBackoff: invalid reconnect backoff (start 1ms, cap 86400.000000001s): "
                + "the cap must be at most one day", e.getMessage());
    }

    // Needs NGROK_AUTHTOKEN and the network, as DataTest does.
    @Test
    public void connectsWithEveryReconnectOptionSet() throws Exception {
        try (var session = Session.withAuthtokenFromEnv()
                .reconnectBackoff(Duration.ofMillis(100), Duration.ofSeconds(5))
                .reconnectJitter(false)
                .reconnectMaxAttempts(5)
                .connect();
                var listener = session.httpEndpoint().listen()) {
            assertTrue(listener.getUrl(), listener.getUrl().startsWith("https://"));
        }
    }
}
