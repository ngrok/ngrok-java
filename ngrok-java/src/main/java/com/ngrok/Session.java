package com.ngrok;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A session with the ngrok service.
 */
public interface Session extends AutoCloseable {
    /**
     * Creates a new session {@link Builder} with specified ngrok authtoken
     *
     * @param authtoken the authtoken
     * @return the builder
     */
    static Builder withAuthtoken(String authtoken) {
        return new Builder(authtoken);
    }

    /**
     * Creates a new session {@link Builder} resolving
     * the ngrok authtoken from {@code NGROK_AUTHTOKEN} env variable
     *
     * @return the builder
     */
    static Builder withAuthtokenFromEnv() {
        return new Builder(System.getenv("NGROK_AUTHTOKEN"));
    }

    /**
     * Connects a session with specified {@link Builder}
     *
     * @param builder the builder
     * @return newly created session
     * @throws IOException if an I/O error occurs
     */
    static Session connect(Builder builder) throws IOException {
        try {
            var clazz = Class.forName("com.ngrok.NativeSession");
            var method = clazz.getMethod("connect", Builder.class);
            return (Session) method.invoke(null, builder);
        } catch (InvocationTargetException e) {
            var cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new RuntimeException(cause);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the ID of this session
     *
     * @return session ID
     */
    String getId();

    /**
     * Returns the metadata of this session
     *
     * @return session metadata
     */
    String getMetadata();

    /**
     * Creates a new {@link TcpBuilder} associated with this session.
     *
     * @return the builder
     */
    default TcpBuilder tcpEndpoint() {
        return new TcpBuilder(this);
    }

    /**
     * Configures and starts a TCP {@link Listener.Endpoint}
     *
     * @param builder the builder
     * @return the running listener
     * @throws IOException if an I/O error occurs
     */
    Listener.Endpoint listenTcp(TcpBuilder builder) throws IOException;

    /**
     * Configures and starts a TCP {@link Forwarder.Endpoint}
     *
     * @param builder the builder
     * @param url the url to forward to
     * @return the running forwarder
     * @throws IOException if an I/O error occurs
     */
    Forwarder.Endpoint forwardTcp(TcpBuilder builder, URL url) throws IOException;

    /**
     * Creates a new {@link TlsBuilder} associated with this session.
     *
     * @return the builder
     */
    default TlsBuilder tlsEndpoint() {
        return new TlsBuilder(this);
    }

    /**
     * Configures and starts a TLS {@link Listener.Endpoint}
     *
     * @param builder the builder
     * @return the running listener
     * @throws IOException if an I/O error occurs
     */
    Listener.Endpoint listenTls(TlsBuilder builder) throws IOException;

    /**
     * Configures and starts a TLS {@link Forwarder.Endpoint}
     *
     * @param builder the builder
     * @param url the url to forward to
     * @return the running forwarder
     * @throws IOException if an I/O error occurs
     */
    Forwarder.Endpoint forwardTls(TlsBuilder builder, URL url) throws IOException;

    /**
     * Creates a new {@link HttpBuilder} associated with this session.
     *
     * @return the builder
     */
    default HttpBuilder httpEndpoint() {
        return new HttpBuilder(this);
    }

    /**
     * Configures and starts a HTTP {@link Listener.Endpoint}
     *
     * @param builder the builder
     * @return the running listener
     * @throws IOException if an I/O error occurs
     */
    Listener.Endpoint listenHttp(HttpBuilder builder) throws IOException;

    /**
     * Configures and starts a HTTP {@link Forwarder.Endpoint}
     *
     * @param builder the builder
     * @param url the url to forward to
     * @return the running forwarder
     * @throws IOException if an I/O error occurs
     */
    Forwarder.Endpoint forwardHttp(HttpBuilder builder, URL url) throws IOException;

    /**
     * Creates a new {@link EdgeBuilder} associated with this session.
     *
     * @return the builder
     */
    default EdgeBuilder edge() {
        return new EdgeBuilder(this);
    }

    /**
     * Configures and starts a {@link Listener.Edge}
     *
     * @param builder the builder
     * @return the running listener
     * @throws IOException if an I/O error occurs
     */
    Listener.Edge listenEdge(EdgeBuilder builder) throws IOException;

    /**
     * Configures and starts a {@link Forwarder.Edge}
     *
     * @param builder the builder
     * @param url the url to forward to
     * @return the running forwarder
     * @throws IOException if an I/O error occurs
     */
    Forwarder.Edge forwardEdge(EdgeBuilder builder, URL url) throws IOException;

    /**
     * Closes a listener by its ID
     *
     * @param listenerId the listener ID
     * @throws IOException if an I/O error occurs
     */
    void closeListener(String listenerId) throws IOException;

    /**
     * Closes a forwarder by its ID
     *
     * @param forwarderId the forwarder ID
     * @throws IOException if an I/O error occurs
     */
    void closeForwarder(String forwarderId) throws IOException;

    @Override
    void close() throws IOException;

    /**
     * Provides a way to listen for specific server side events.
     */
    interface CommandHandler {
        /**
         * Called when the associated event triggers.
         */
        void onCommand();
    }

    /**
     * Provides a way to monitor current session's heartbeats and disconnects.
     */
    interface HeartbeatHandler {
        /**
         * Called on each successful heartbeat, with the duration it took to execute it.
         *
         * @param durationMs the duration of the heartbeat in milliseconds
         */
        void heartbeat(long durationMs);

        /**
         * Called when session times out (e.g. the heartbeat fails). The session will
         * automatically reconnect, but this gives the application a chance to react.
         */
        default void timeout() {}
    }

    /**
     * Represents additional information about the client. Use it to describe your application.
     *
     * This library also injects its own client information, describing lower levels of the stack.
     */
    class ClientInfo {
        private final String type;

        private final String version;

        private final Optional<String> comments;

        /**
         * Creates a new client information with a given type, version and comment.
         *
         * @param type the type of the client, required
         * @param version the version of the client, required
         * @param comments additional comments, optional
         */
        public ClientInfo(String type, String version, String comments) {
            this.type = Objects.requireNonNull(type);
            this.version = Objects.requireNonNull(version);
            this.comments = Optional.ofNullable(comments);
        }

        /**
         * Returns the type of this client.
         *
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * Returns the version of this client.
         *
         * @return the version
         */
        public String getVersion() {
            return version;
        }

        /**
         * Returns the comments for this client.
         *
         * @return the comments
         */
        public Optional<String> getComments() {
            return comments;
        }
    }

    /**
     * A builder for creating a session
     */
    class Builder {

        private final String authtoken;

        private Optional<Duration> heartbeatInterval = Optional.empty();
        private Optional<Duration> heartbeatTolerance = Optional.empty();

        // Both set together, by reconnectBackoff.
        private Optional<Duration> reconnectBackoffInitial = Optional.empty();
        private Optional<Duration> reconnectBackoffMax = Optional.empty();
        private Optional<Boolean> reconnectJitter = Optional.empty();
        private Optional<Integer> reconnectMaxAttempts = Optional.empty();

        private Optional<String> metadata = Optional.empty();

        private Optional<String> serverAddr = Optional.empty();
        private byte[] caCert;

        private CommandHandler stopCallback;
        private CommandHandler restartCallback;
        private CommandHandler updateCallback;

        private HeartbeatHandler heartbeatHandler;

        private final List<ClientInfo> clientInfos = new ArrayList<>();

        private Builder(String authtoken) {
            this.authtoken = Objects.requireNonNullElse(authtoken, "");
        }

        /**
         * Sets the heartbeat interval for this builder
         *
         * @param duration the interval duration
         * @return the builder instance
         */
        public Builder heartbeatInterval(Duration duration) {
            this.heartbeatInterval = Optional.of(duration);
            return this;
        }

        /**
         * Sets the heartbeat tolerance for this builder
         *
         * @param duration the tolerance duration
         * @return the builder instance
         */
        public Builder heartbeatTolerance(Duration duration) {
            this.heartbeatTolerance = Optional.of(duration);
            return this;
        }

        /**
         * Sets how long the session waits between attempts to reconnect after it
         * loses its connection to the ngrok service.
         * <p>
         * The first attempt is immediate. After it, the session waits {@code initial},
         * then twice as long before each attempt, up to {@code max}. Each wait is
         * scaled by a random factor in [0, 1) unless {@link #reconnectJitter(boolean)}
         * turns that off. The defaults are 50 ms and 30 s. For a fixed wait between
         * every attempt, pass the same value for both and turn jitter off.
         * <p>
         * These waits never stop the session from reconnecting on their own:
         * {@link #reconnectMaxAttempts(int)}, or closing the session, does.
         * <p>
         * The ngrok Rust SDK checks the range when the session connects: an
         * {@code initial} below 1 ms or above {@code max}, or a {@code max} above one
         * day, makes {@link #connect()} throw an {@link IOException} whose message
         * starts with "reconnectBackoff: " and names the rule broken.
         *
         * @param initial the first wait between reconnect attempts
         * @param max the longest wait between reconnect attempts
         * @return the builder instance
         * @throws NullPointerException if either argument is null
         * @throws IllegalArgumentException if either argument is negative; the
         *         builder is then unchanged
         */
        public Builder reconnectBackoff(Duration initial, Duration max) {
            Objects.requireNonNull(initial, "initial");
            Objects.requireNonNull(max, "max");
            if (initial.isNegative()) {
                throw new IllegalArgumentException("initial must not be negative, got " + initial);
            }
            if (max.isNegative()) {
                throw new IllegalArgumentException("max must not be negative, got " + max);
            }
            this.reconnectBackoffInitial = Optional.of(initial);
            this.reconnectBackoffMax = Optional.of(max);
            return this;
        }

        /**
         * Sets whether each wait between reconnect attempts is scaled by a random
         * factor in [0, 1). On by default, so that sessions that lose their
         * connections together don't all reconnect at the same moment.
         *
         * @param enabled whether to scale each wait by a random factor
         * @return the builder instance
         */
        public Builder reconnectJitter(boolean enabled) {
            this.reconnectJitter = Optional.of(enabled);
            return this;
        }

        /**
         * Sets the session to stop reconnecting after {@code attempts} failed attempts
         * in one outage, counting the immediate first attempt. Each new outage starts
         * the count again. By default the session never stops on its own.
         * <p>
         * When the session stops, every listener and forwarder stops with the last
         * attempt's error: {@link Listener#accept()} and {@link Forwarder#join()}
         * throw an {@link IOException}.
         *
         * @param attempts the number of failed attempts after which to stop
         * @return the builder instance
         * @throws IllegalArgumentException if {@code attempts} is below 1; the builder
         *         is then unchanged
         */
        public Builder reconnectMaxAttempts(int attempts) {
            if (attempts < 1) {
                throw new IllegalArgumentException("attempts must be at least 1, got " + attempts);
            }
            this.reconnectMaxAttempts = Optional.of(attempts);
            return this;
        }

        /**
         * Sets the metadata for this builder
         *
         * @param metadata the metadata
         * @return the builder instance
         */
        public Builder metadata(String metadata) {
            this.metadata = Optional.of(metadata);
            return this;
        }

        /**
         * Sets the server address for this builder
         *
         * @param addr the server address
         * @return the builder instance
         */
        public Builder serverAddr(String addr) {
            this.serverAddr = Optional.of(addr);
            return this;
        }

        /**
         * Sets the ca certificate for this builder
         *
         * @param data the ca certificate
         * @return the builder instance
         */
        public Builder caCert(byte[] data) {
            this.caCert = data;
            return this;
        }

        /**
         * Sets the stop callback handler for this builder
         *
         * @param callback the stop callback
         * @return the builder instance
         */
        public Builder stopCallback(CommandHandler callback) {
            this.stopCallback = callback;
            return this;
        }

        /**
         * Sets the restart callback handler for this builder
         *
         * @param callback the restart callback
         * @return the builder instance
         */
        public Builder restartCallback(CommandHandler callback) {
            this.restartCallback = callback;
            return this;
        }

        /**
         * Sets the update callback handler for this builder
         *
         * @param callback the update callback
         * @return the builder instance
         */
        public Builder updateCallback(CommandHandler callback) {
            this.updateCallback = callback;
            return this;
        }

        /**
         * Sets the heartbeat handler for this builder
         *
         * @param heartbeatHandler the heartbeat callback
         * @return the builder instance
         */
        public Builder heartbeatHandler(HeartbeatHandler heartbeatHandler) {
            this.heartbeatHandler = heartbeatHandler;
            return this;
        }

        /**
         * Adds a client info to the list of client info objects for this builder
         *
         * @param name the client name
         * @param version the client version
         * @return the builder instance
         */
        public Builder addClientInfo(String name, String version) {
            this.clientInfos.add(new ClientInfo(name, version, null));
            return this;
        }

        /**
         * Adds a client info to the list of client info objects for this builder
         *
         * @param name the client name
         * @param version the client version
         * @param comments the comments
         * @return the builder instance
         */
        public Builder addClientInfo(String name, String version, String comments) {
            this.clientInfos.add(new ClientInfo(name, version, comments));
            return this;
        }

        /**
         * Returns the ngrok authtoken associated with this builder.
         *
         * @return the authtoken
         */
        public String getAuthtoken() {
            return authtoken;
        }

        /**
         * Returns the heartbeat interval for this builder
         *
         * @return the heartbeat interval
         */
        public Optional<Duration> getHeartbeatInterval() {
            return heartbeatInterval;
        }

        /**
         * Returns the heartbeat tolerance for this builder
         *
         * @return the heartbeat tolerance
         */
        public Optional<Duration> getHeartbeatTolerance() {
            return heartbeatTolerance;
        }

        /**
         * Returns the first wait between reconnect attempts set for this builder.
         *
         * @return the first wait, if {@link #reconnectBackoff(Duration, Duration)} set one
         */
        public Optional<Duration> getReconnectBackoffInitial() {
            return reconnectBackoffInitial;
        }

        /**
         * Returns the longest wait between reconnect attempts set for this builder.
         *
         * @return the longest wait, if {@link #reconnectBackoff(Duration, Duration)} set one
         */
        public Optional<Duration> getReconnectBackoffMax() {
            return reconnectBackoffMax;
        }

        /**
         * Returns whether reconnect waits are scaled by a random factor, as set for
         * this builder.
         *
         * @return the setting, if {@link #reconnectJitter(boolean)} set one
         */
        public Optional<Boolean> getReconnectJitter() {
            return reconnectJitter;
        }

        /**
         * Returns the number of failed reconnect attempts after which the session
         * stops, as set for this builder.
         *
         * @return the number, if {@link #reconnectMaxAttempts(int)} set one
         */
        public Optional<Integer> getReconnectMaxAttempts() {
            return reconnectMaxAttempts;
        }

        /**
         * Returns the metadata for this builder.
         *
         * @return the metadata
         */
        public Optional<String> getMetadata() {
            return metadata;
        }

        /**
         * Returns the server address for this builder.
         *
         * @return the server address
         */
        public Optional<String> getServerAddr() {
            return serverAddr;
        }

        /**
         * Returns the certificate for this builder.
         *
         * @return the certificate
         */
        public byte[] getCaCert() {
            return caCert;
        }

        /**
         * Returns the stop callback handler for this builder.
         *
         * @return the stop handler
         */
        public CommandHandler stopCallback() {
            return stopCallback;
        }

        /**
         * Returns the restart callback handler for this builder.
         *
         * @return the restart handler
         */
        public CommandHandler restartCallback() {
            return restartCallback;
        }

        /**
         * Returns the update callback handler for this builder.
         *
         * @return the update handler
         */
        public CommandHandler updateCallback() {
            return updateCallback;
        }

        /**
         * Returns the heartbeat handler for this builder.
         *
         * @return the heartbeat handler
         */
        public HeartbeatHandler heartbeatHandler() {
            return heartbeatHandler;
        }

        /**
         * Returns the list of client info objects to add for this builder
         *
         * @return the list of client info objects
         */
        public List<ClientInfo> getClientInfos() {
            return clientInfos;
        }

        /**
         * Connects a session with the current {@link Builder}
         *
         * @return newly created session
         * @throws IOException if an I/O error occurs
         */
        public Session connect() throws IOException {
            return Session.connect(this);
        }
    }
}