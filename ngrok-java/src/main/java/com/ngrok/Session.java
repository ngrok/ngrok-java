package com.ngrok;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public interface Session extends AutoCloseable {

    public static Builder newBuilder() {
        return newBuilder(System.getenv("NGROK_AUTHTOKEN"));
    }

    public static Builder newBuilder(String authtoken) {
        return new Builder().authtoken(authtoken);
    }

    public static Session connect(Builder builder) throws IOException {
        try {
            var clazz = Class.forName("com.ngrok.NativeSession");
            var method = clazz.getMethod("connect", Builder.class);
            return (Session) method.invoke(null, builder);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IOException ioexc) {
                throw ioexc;
            }
            throw new RuntimeException(e.getCause());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getMetadata();

    public default TcpTunnel tcpTunnel() throws IOException {
        return tcpTunnel(new TcpTunnel.Builder());
    }

    public TcpTunnel tcpTunnel(TcpTunnel.Builder builder) throws IOException;

    public default TlsTunnel tlsTunnel() throws IOException {
        return tlsTunnel(new TlsTunnel.Builder());
    }

    public TlsTunnel tlsTunnel(TlsTunnel.Builder builder) throws IOException;

    public default HttpTunnel httpTunnel() throws IOException {
        return httpTunnel(new HttpTunnel.Builder());
    }
    
    public HttpTunnel httpTunnel(HttpTunnel.Builder builder) throws IOException;

    public default LabeledTunnel labeledTunnel() throws IOException {
        return labeledTunnel(new LabeledTunnel.Builder());
    }

    public LabeledTunnel labeledTunnel(LabeledTunnel.Builder builder) throws IOException;

    public interface StopCallback {
        public void stop();
    }

    public interface RestartCallback {
        public void restart();
    }

    public interface UpdateCallback {
        public void update();
    }

    public interface HeartbeatHandler {
        public void heartbeat(long durationMs);

        public default void timeout() {}
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
        private final List<UserAgent> userAgents = new ArrayList<>();

        private String authtoken;

        private String metadata;

        private StopCallback stopCallback;
        private RestartCallback restartCallback;
        private UpdateCallback updateCallback;

        private HeartbeatHandler heartbeatHandler;

        public Builder() {
        }

        public Builder addUserAgent(String name, String version) {
            this.userAgents.add(new UserAgent(name, version));
            return this;
        }

        public List<UserAgent> getUserAgents() {
            return userAgents;
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
    }
}