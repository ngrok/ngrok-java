package com.ngrok;

import java.io.IOException;
import java.util.Properties;

/**
 * A class representing a session for an ngrok tunnel with native implementation.
 */
public class NativeSession implements Session {
    /**
     * The version of the ngrok agent.
     */
    private static String version = "0.0.0-UNKNOWN";

    static {
        try {
            Runtime.load();
            Runtime.init(new Runtime.Logger());
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

    /**
     * The native address of the session.
     */
    private long native_address;

    /**
     * The metadata of the session.
     */
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
     * Connects to an ngrok tunnel using the specified session builder.
     *
     * @param builder the session builder to use for the connection
     * @return a new NativeSession object representing the connected session
     * @throws IOException if an I/O error occurs
     */
    public static NativeSession connect(Session.Builder builder) throws IOException {
        builder.getUserAgents().add(0, new UserAgent("ngrok-java", version));
        return connectNative(builder);
    }

    /**
     * Connects to an ngrok tunnel using the specified session builder using native
     * code.
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

    /**
     * Creates a new {@link NativeTcpTunnel} using the specified builder.
     *
     * @param builder the builder to use for the tunnel
     * @return a new NativeTcpTunnel object representing the created tunnel
     * @throws IOException if an I/O error occurs
     */
    public native NativeTcpTunnel tcpTunnel(TcpTunnel.Builder builder) throws IOException;

    /**
     * Creates a new {@link NativeTlsTunnel} using the specified builder.
     *
     * @param builder the builder to use for the tunnel
     * @return a new NativeTlsTunnel object representing the created tunnel
     * @throws IOException if an I/O error occurs
     */
    public native NativeTlsTunnel tlsTunnel(NativeTlsTunnel.Builder builder) throws IOException;

    /**
     * Creates a new {@link NativeHttpTunnel} using the specified builder.
     *
     * @param builder the builder to use for the tunnel
     * @return a new NativeHttpTunnel object representing the created tunnel
     * @throws IOException if an I/O error occurs
     */
    public native NativeHttpTunnel httpTunnel(NativeHttpTunnel.Builder builder) throws IOException;

    /**
     * Creates a new {@link NativeLabeledTunnel} using the specified builder.
     *
     * @param builder the builder to use for the tunnel
     * @return a new NativeLabeledTunnel object representing the created tunnel
     * @throws IOException if an I/O error occurs
     */
    public native NativeLabeledTunnel labeledTunnel(NativeLabeledTunnel.Builder builder) throws IOException;

    /**
     * Closes the native session.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public native void close() throws IOException;
}