package com.ngrok;

import java.io.IOException;

/**
 * A class representing an HTTP tunnel with native implementation.
 */
public class NativeHttpTunnel extends HttpTunnel {
    /**
     * The native address of the tunnel.
     */
    private long native_address;

    /**
     * Constructs a new HTTP tunnel with the specified ID, forwarding address,
     * metadata, protocol, and URL.
     *
     * @param id         the ID of the tunnel
     * @param forwardsTo the forwarding address of the tunnel
     * @param metadata   the metadata of the tunnel
     * @param proto      the protocol of the tunnel
     * @param url        the URL of the tunnel
     */
    public NativeHttpTunnel(String id, String forwardsTo, String metadata, String proto, String url) {
        super(id, forwardsTo, metadata, proto, url);
    }

    /**
     * Accepts a new connection on the HTTP tunnel.
     *
     * @return a new NativeConnection object representing the accepted connection
     * @throws IOException if an I/O error occurs
     */
    @Override
    public native NativeConnection accept() throws IOException;

    /**
     * Forwards TCP traffic to the specified address.
     *
     * @param addr the address to forward TCP traffic to
     * @throws IOException if an I/O error occurs
     */
    @Override
    public native void forwardTcp(String addr) throws IOException;

    /**
     * Closes the HTTP tunnel.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public native void close() throws IOException;
}