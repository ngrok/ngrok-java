package com.ngrok;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

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

    public String metadata();

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
        public final String name;

        public final String version;

        public UserAgent(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String name() {
            return name;
        }

        public String version() {
            return version;
        }
    }

    public static class Builder {
        public final List<UserAgent> userAgents = new ArrayList<>();

        public String authtoken;

        public String metadata;

        public StopCallback stopCallback;
        public RestartCallback restartCallback;
        public UpdateCallback updateCallback;

        public HeartbeatHandler heartbeatHandler;

        public Builder() {
        }

        public Builder addUserAgent(String name, String version) {
            this.userAgents.add(new UserAgent(name, version));
            return this;
        }

        public Builder authtoken(String authtoken) {
            this.authtoken = authtoken;
            return this;
        }

        public Builder metadata(String metadata) {
            this.metadata = metadata;
            return this;
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