package com.ngrok;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A session with the ngrok service.
 */
public interface Session extends AutoCloseable {

    /**
     * Returns a new {@link Builder} instance with your ngrok authentication token
     * from the environment.
     *
     * @return a new {@link Builder} instance with the default authentication token
     */
    public static Builder newBuilder() {
        return newBuilder(System.getenv("NGROK_AUTHTOKEN"));
    }

    /**
     * Returns a new {@link Builder} instance with the specified ngrok
     * authentication token.
     *
     * @param authtoken the authentication token to use
     * @return a new {@link Builder} instance with the specified authentication
     *         token
     */
    public static Builder newBuilder(String authtoken) {
        return new Builder().authtoken(authtoken);
    }

    /**
     * Connects to the ngrok service using the specified {@link Builder} instance.
     *
     * @param builder the {@link Builder} instance to use for the connection
     * @return a new {@link Session} instance connected to the ngrok service
     * @throws IOException if an I/O error occurs during the connection
     */
    public static Session connect(Builder builder) throws IOException {
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
     * Returns the metadata for the session.
     *
     * @return the metadata for the session
     */
    public String getMetadata();

    /**
     * Returns a new {@link TcpTunnel} instance with the default settings.
     *
     * Returns a new {@link TcpTunnel} instance with the default settings
     * 
     * @throws IOException if an I/O error occurs during the tunnel creation
     */
    public default TcpTunnel tcpTunnel() throws IOException {
        return tcpTunnel(new TcpTunnel.Builder());
    }

    /**
     * Returns a new {@link TcpTunnel} instance with the specified settings.
     *
     * @param builder the {@link TcpTunnel.Builder} instance to use for the tunnel
     *                creation
     *                Returns a new {@link TcpTunnel} instance with the specified
     *                settings
     * @throws IOException if an I/O error occurs during the tunnel creation
     */
    public TcpTunnel tcpTunnel(TcpTunnel.Builder builder) throws IOException;

    /**
     * Creates a new {@link TlsTunnel} instance with the default settings.
     *
     * @returns a new {@link TlsTunnel} instance with the default settings
     * @throws IOException if an I/O error occurs during the tunnel creation
     */
    public default TlsTunnel tlsTunnel() throws IOException {
        return tlsTunnel(new TlsTunnel.Builder());
    }

    /**
     * Creates a new {@link TlsTunnel} instance with the specified settings.
     *
     * @param builder the {@link TlsTunnel.Builder} instance to use for the tunnel
     *                creation
     * @returns a new {@link TlsTunnel} instance with the specified settings
     * @throws IOException if an I/O error occurs during the tunnel creation
     */
    public TlsTunnel tlsTunnel(TlsTunnel.Builder builder) throws IOException;

    /**
     * Creates a new {@link HttpTunnel} instance with the default settings.
     *
     * @return a new {@link HttpTunnel} instance with the default settings
     * @throws IOException if an I/O error occurs during the tunnel creation
     */
    public default HttpTunnel httpTunnel() throws IOException {
        return httpTunnel(new HttpTunnel.Builder());
    }

    /**
     * Creates a new {@link HttpTunnel} instance with the specified settings.
     *
     * @param builder the {@link HttpTunnel.Builder} instance to use for the tunnel
     *                creation
     * @return a new {@link HttpTunnel} instance with the specified settings
     * @throws IOException if an I/O error occurs during the tunnel creation
     */
    public HttpTunnel httpTunnel(HttpTunnel.Builder builder) throws IOException;

    /**
     * Returns a new {@link LabeledTunnel} instance with the default settings.
     *
     * @return a new {@link LabeledTunnel} instance with the default settings
     * @throws IOException if an I/O error occurs during the tunnel creation
     */
    public default LabeledTunnel labeledTunnel() throws IOException {
        return labeledTunnel(new LabeledTunnel.Builder());
    }

    /**
     * Returns a new {@link LabeledTunnel} instance with the specified settings.
     *
     * @param builder the {@link LabeledTunnel.Builder} instance to use for the tunnel
     *                creation
     * @return a new {@link LabeledTunnel} instance with the specified settings
     * @throws IOException if an I/O error occurs during the tunnel creation
     */
    public LabeledTunnel labeledTunnel(LabeledTunnel.Builder builder) throws IOException;

    /**
     * Represents a callback for stopping a session. Implements 
     */
    public interface StopCallback {
        public void stop();
    }

    /**
     * The `RestartCallback` interface represents a callback for restarting a
     * session.
     */
    public interface RestartCallback {
        public void restart();
    }

    /**
     * The `UpdateCallback` interface represents a callback for updating a session.
     */
    public interface UpdateCallback {
        public void update();
    }

    /**
     * The `HeartbeatHandler` interface represents a handler for session heartbeats.
     */
    public interface HeartbeatHandler {
        /**
         * Handles a session heartbeat with the specified duration.
         *
         * @param durationMs the duration of the heartbeat in milliseconds
         */
        public void heartbeat(long durationMs);

        /**
         * Handles a session heartbeat timeout.
         */
        public default void timeout() {
        }
    }

    class UserAgent {
        private final String name;

        private final String version;

        public UserAgent(String name, String version) {
            this.name = Objects.requireNonNull(name);
            this.version = Objects.requireNonNull(version);
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }
    }

    public static class Builder {

        private String authtoken;

        private Duration heartbeatInterval;
        private Duration heartbeatTolerance;

        private String metadata;

        private String serverAddr;
        private byte[] caCert;

        private StopCallback stopCallback;
        private RestartCallback restartCallback;
        private UpdateCallback updateCallback;

        private HeartbeatHandler heartbeatHandler;

        private final List<UserAgent> userAgents = new ArrayList<>();

        public Builder() {
        }

        public Builder authtoken(String authtoken) {
            this.authtoken = authtoken;
            return this;
        }

        public boolean hasAuthtoken() {
            return authtoken != null;
        }

        public String getAuthtoken() {
            return authtoken;
        }

        public Builder heartbeatInterval(Duration duration) {
            this.heartbeatInterval = duration;
            return this;
        }

        public boolean hasHeartbeatInterval() {
            return heartbeatInterval != null;
        }

        public long getHeartbeatIntervalMs() {
            return heartbeatInterval.toMillis();
        }

        public Builder heartbeatTolerance(Duration duration) {
            this.heartbeatTolerance = duration;
            return this;
        }

        public boolean hasHeartbeatTolerance() {
            return heartbeatTolerance != null;
        }

        public long getHeartbeatToleranceMs() {
            return heartbeatTolerance.toMillis();
        }

        public Builder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public boolean hasMetadata() {
            return metadata != null;
        }

        public String getMetadata() {
            return metadata;
        }

        public Builder serverAddr(String addr) {
            this.serverAddr = addr;
            return this;
        }

        public boolean hasServerAddr() {
            return serverAddr != null;
        }

        public String getServerAddr() {
            return serverAddr;
        }

        public Builder caCert(byte[] data) {
            this.caCert = data;
            return this;
        }

        public byte[] getCaCert() {
            return caCert;
        }

        public Builder stopCallback(StopCallback callback) {
            this.stopCallback = callback;
            return this;
        }

        public StopCallback stopCallback() {
            return stopCallback;
        }

        public Builder restartCallback(RestartCallback callback) {
            this.restartCallback = callback;
            return this;
        }

        public RestartCallback restartCallback() {
            return restartCallback;
        }

        public Builder updateCallback(UpdateCallback callback) {
            this.updateCallback = callback;
            return this;
        }

        public UpdateCallback updateCallback() {
            return updateCallback;
        }

        public Builder heartbeatHandler(HeartbeatHandler heartbeatHandler) {
            this.heartbeatHandler = heartbeatHandler;
            return this;
        }

        public HeartbeatHandler heartbeatHandler() {
            return heartbeatHandler;
        }

        public Builder addUserAgent(String name, String version) {
            this.userAgents.add(new UserAgent(name, version));
            return this;
        }

        public List<UserAgent> getUserAgents() {
            return userAgents;
        }
    }
}