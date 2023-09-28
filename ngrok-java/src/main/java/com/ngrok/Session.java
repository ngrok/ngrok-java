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
    static Builder withAuthtoken(String authtoken) {
        return new Builder(authtoken);
    }

    static Builder withAuthtokenFromEnv() {
        return new Builder(System.getenv("NGROK_AUTHTOKEN"));
    }

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

    String getMetadata();

    default TcpBuilder tcpEndpoint() {
        return new TcpBuilder(this);
    }

    Listener.Endpoint listenTcp(TcpBuilder builder) throws IOException;

    Forwarder.Endpoint forwardTcp(TcpBuilder builder, URL url) throws IOException;

    default TlsBuilder tlsEndpoint() {
        return new TlsBuilder(this);
    }

    Listener.Endpoint listenTls(TlsBuilder builder) throws IOException;

    Forwarder.Endpoint forwardTls(TlsBuilder builder, URL url) throws IOException;

    default HttpBuilder httpEndpoint() {
        return new HttpBuilder(this);
    }

    Listener.Endpoint listenHttp(HttpBuilder builder) throws IOException;

    Forwarder.Endpoint forwardHttp(HttpBuilder builder, URL url) throws IOException;

    default EdgeBuilder edge() {
        return new EdgeBuilder(this);
    }

    Listener.Edge listenEdge(EdgeBuilder builder) throws IOException;

    Forwarder.Edge forwardEdge(EdgeBuilder builder, URL url) throws IOException;

    void closeListener(String listenerId) throws IOException;

    void closeForwarder(String forwarderId) throws IOException;

    @Override
    void close() throws IOException;

    /**
     * Configures a function which is called when the ngrok service requests that this {@link Session} stops.
     * Your application may choose to interpret this callback as a request to terminate the {@link Session} or the entire process.
     */
    public interface StopCallback {
        void onStopCommand();
    }

    /**
     * Configures a function which is called when the ngrok service requests that this {@link Session} updates.
     * Your application may choose to interpret this callback as a request to restart the {@link Session} or the entire process.
     */
    public interface RestartCallback {
        void onRestartCommand();
    }

    /**
     * Configures a function which is called when the ngrok service requests that this {@link Session} updates.
     * Your application may choose to interpret this callback as a request to update its configuration, itself, or to invoke some other application-specific behavior.
     * 
     */
    interface UpdateCallback {
        void onUpdateCommand();
    }

    /**
     * The `HeartbeatHandler` interface represents a handler for session heartbeats.
     */
    interface HeartbeatHandler {
        /**
         * Handles a session heartbeat with the specified duration.
         *
         * @param durationMs the duration of the heartbeat in milliseconds
         */
        void heartbeat(long durationMs);

        /**
         * Handles a session heartbeat timeout.
         */
        default void timeout() {}
    }

    class ClientInfo {
        private final String type;

        private final String version;

        private final String comments;

        public ClientInfo(String type, String version, String comments) {
            this.type = Objects.requireNonNull(type);
            this.version = Objects.requireNonNull(version);
            this.comments = comments;
        }

        public String getType() {
            return type;
        }

        public String getVersion() {
            return version;
        }

        public String getComments() {
            return comments;
        }

        public boolean hasComments() {
            return comments != null;
        }
    }

    class Builder {

        private final String authtoken;

        private Duration heartbeatInterval;
        private Duration heartbeatTolerance;

        private Optional<String> metadata;

        private String serverAddr;
        private byte[] caCert;

        private StopCallback stopCallback;
        private RestartCallback restartCallback;
        private UpdateCallback updateCallback;

        private HeartbeatHandler heartbeatHandler;

        private final List<ClientInfo> clientInfos = new ArrayList<>();

        private Builder(String authtoken) {
            this.authtoken = Objects.requireNonNullElse(authtoken, "");
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
            this.metadata = Optional.of(metadata);
            return this;
        }

        public Optional<String> getMetadata() {
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

        public Builder addClientInfo(String name, String version) {
            this.clientInfos.add(new ClientInfo(name, version, null));
            return this;
        }

        public Builder addClientInfo(String name, String version, String comments) {
            this.clientInfos.add(new ClientInfo(name, version, comments));
            return this;
        }

        public List<ClientInfo> getClientInfos() {
            return clientInfos;
        }

        public Session connect() throws IOException {
            return Session.connect(this);
        }
    }
}