package com.ngrok;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

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

    public static class Builder {
        protected String version;

        public String authtoken;

        public String metadata;

        public StopCallback stopCallback;
        public RestartCallback restartCallback;
        public UpdateCallback updateCallback;

        public Builder() {
        }

        public Builder authtoken(String authtoken) {
            this.authtoken = authtoken;
            return this;
        }

        public Builder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder setStopCallback(StopCallback callback) {
            this.stopCallback = callback;
            return this;
        }

        public Builder setRestartCallback(RestartCallback callback) {
            this.restartCallback = callback;
            return this;
        }

        public Builder setUpdateCallback(UpdateCallback callback) {
            this.updateCallback = callback;
            return this;
        }
    }
}