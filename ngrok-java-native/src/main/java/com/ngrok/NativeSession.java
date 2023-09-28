package com.ngrok;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * An implementation of {@link Session} that delegates calls to a native library.
 */
public class NativeSession implements Session {
    private static String version = "0.0.0-UNKNOWN";

    static {
        try {
            Runtime.load();
            Runtime.init(Runtime.getLogger());
        } catch (Throwable th) {
            // TODO better error handling here?
            th.printStackTrace();
        }

        try {
            Properties props = new Properties();
            props.load(NativeSession.class.getResourceAsStream("/native.properties"));
            version = props.getProperty("agent.version", "0.0.0-SNAPSHOT");
        } catch (Throwable th) {
            // TODO better error handling here?
            th.printStackTrace();
        }
    }

    private long native_address;
    private final String metadata;

    /**
     * Constructs a new native session with the specified metadata.
     *
     * @param metadata the metadata of the session
     */
    public NativeSession(String metadata) {
        this.metadata = metadata;
    }

    /**
     * Establishes an ngrok session specified by the provided builder.
     * Wraps {@link NativeSession#connectNative(Session.Builder)}.
     *
     * @param builder the session builder to use for the connection
     * @return a new NativeSession object representing the connected session
     * @throws IOException if an I/O error occurs
     */
    public static NativeSession connect(Session.Builder builder) throws IOException {
        var jver = System.getProperty("java.version");
        builder.getClientInfos().add(0, new ClientInfo("ngrok-java", version, jver));
        return connectNative(builder);
    }

    /**
     * Establishes an ngrok session specified by the provided builder.
     *
     * @param builder the session builder to use for the connection
     * @return a new {@link NativeSession} object representing the connected session
     * @throws IOException if an I/O error occurs
     */
    public static native NativeSession connectNative(Session.Builder builder) throws IOException;

    /**
     * Returns the metadata of the session.
     *
     * @return the metadata of the session
     */
    @Override
    public String getMetadata() {
        return metadata;
    }

    @Override
    public native NativeTcpListener listenTcp(TcpBuilder builder) throws IOException;

    @Override
    public native NativeTcpForwarder forwardTcp(TcpBuilder builder, URL url) throws IOException;

    @Override
    public native NativeTlsListener listenTls(TlsBuilder builder) throws IOException;

    @Override
    public native NativeTlsForwarder forwardTls(TlsBuilder builder, URL url) throws IOException;

    @Override
    public native NativeHttpListener listenHttp(HttpBuilder builder) throws IOException;

    @Override
    public native NativeHttpForwarder forwardHttp(HttpBuilder builder, URL url) throws IOException;

    @Override
    public native NativeEdgeListener listenEdge(EdgeBuilder builder) throws IOException;

    @Override
    public native NativeEdgeForwarder forwardEdge(EdgeBuilder builder, URL url) throws IOException;

    @Override
    public native void closeListener(String id) throws IOException;

    @Override
    public native void closeForwarder(String id) throws IOException;

    /**
     * Closes the native session.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public native void close() throws IOException;
}