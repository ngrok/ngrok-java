package com.ngrok;

import org.junit.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.Assert.*;

// The range rules (a start of at least 1 ms and at most the cap, a cap of at
// most one day) are the ngrok Rust SDK's, checked when the session connects;
// ngrok-java-native's ReconnectTest covers them. These are the refusals the
// setters make themselves, for what the Rust types cannot hold.
public class SessionBuilderTest {
    private static Session.Builder builder() {
        return Session.withAuthtoken("unused");
    }

    @Test
    public void reconnectOptionsAreUnsetByDefault() {
        var builder = builder();
        assertEquals(Optional.empty(), builder.getReconnectBackoffInitial());
        assertEquals(Optional.empty(), builder.getReconnectBackoffMax());
        assertEquals(Optional.empty(), builder.getReconnectJitter());
        assertEquals(Optional.empty(), builder.getReconnectMaxAttempts());
    }

    @Test
    public void reconnectBackoffKeepsBothDurationsExactly() {
        var initial = Duration.ofNanos(1_500_001);
        var max = Duration.ofDays(1);
        var builder = builder();
        assertSame(builder, builder.reconnectBackoff(initial, max));
        assertEquals(Optional.of(Duration.ofNanos(1_500_001)), builder.getReconnectBackoffInitial());
        assertEquals(Optional.of(Duration.ofDays(1)), builder.getReconnectBackoffMax());
    }

    @Test
    public void reconnectBackoffRefusesANegativeInitial() {
        var builder = builder();
        var e = assertThrows(IllegalArgumentException.class,
                () -> builder.reconnectBackoff(Duration.ofMillis(-1), Duration.ofSeconds(1)));
        assertEquals("initial must not be negative, got PT-0.001S", e.getMessage());
        assertEquals(Optional.empty(), builder.getReconnectBackoffInitial());
        assertEquals(Optional.empty(), builder.getReconnectBackoffMax());
    }

    @Test
    public void reconnectBackoffRefusesANegativeMax() {
        var builder = builder();
        var e = assertThrows(IllegalArgumentException.class,
                () -> builder.reconnectBackoff(Duration.ofMillis(1), Duration.ofSeconds(-1)));
        assertEquals("max must not be negative, got PT-1S", e.getMessage());
        assertEquals(Optional.empty(), builder.getReconnectBackoffInitial());
    }

    @Test
    public void reconnectBackoffRefusesNull() {
        var builder = builder();
        var e = assertThrows(NullPointerException.class,
                () -> builder.reconnectBackoff(null, Duration.ofSeconds(1)));
        assertEquals("initial", e.getMessage());
        e = assertThrows(NullPointerException.class,
                () -> builder.reconnectBackoff(Duration.ofSeconds(1), null));
        assertEquals("max", e.getMessage());
        assertEquals(Optional.empty(), builder.getReconnectBackoffInitial());
    }

    @Test
    public void reconnectJitterKeepsTheSetting() {
        var builder = builder();
        assertSame(builder, builder.reconnectJitter(false));
        assertEquals(Optional.of(false), builder.getReconnectJitter());
        builder.reconnectJitter(true);
        assertEquals(Optional.of(true), builder.getReconnectJitter());
    }

    @Test
    public void reconnectMaxAttemptsKeepsOneAndAbove() {
        var builder = builder();
        assertSame(builder, builder.reconnectMaxAttempts(1));
        assertEquals(Optional.of(1), builder.getReconnectMaxAttempts());
        builder.reconnectMaxAttempts(Integer.MAX_VALUE);
        assertEquals(Optional.of(Integer.MAX_VALUE), builder.getReconnectMaxAttempts());
    }

    @Test
    public void reconnectMaxAttemptsRefusesZero() {
        var builder = builder();
        var e = assertThrows(IllegalArgumentException.class, () -> builder.reconnectMaxAttempts(0));
        assertEquals("attempts must be at least 1, got 0", e.getMessage());
        assertEquals(Optional.empty(), builder.getReconnectMaxAttempts());
    }

    @Test
    public void reconnectMaxAttemptsRefusesANegativeCount() {
        var builder = builder();
        var e = assertThrows(IllegalArgumentException.class, () -> builder.reconnectMaxAttempts(-1));
        assertEquals("attempts must be at least 1, got -1", e.getMessage());
    }
}
