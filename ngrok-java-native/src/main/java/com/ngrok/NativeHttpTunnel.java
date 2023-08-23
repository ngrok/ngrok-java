package com.ngrok;

/**
* An implementation of {@link HttpTunnel} that delegates calls to a native library.
*/
public class NativeHttpTunnel extends HttpTunnel {
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
     * Accepts a new connection on the HTTP tunnel. Blocks until one is available.
     *
     * @return a new NativeConnection object representing the accepted connection
     * @throws Error if an I/O error occurs
     */
    @Override
    public native NativeConnection accept() throws Error;

    /**
     * Forwards TCP traffic to the specified address. Blocking until closed.
     *
     * @param addr the address to forward TCP traffic to. Example: 127.0.0.1
     * @throws Error if an I/O error occurs
     */
    @Override
    public native void forwardTcp(String addr) throws Error;

    /**
     * Closes the HTTP tunnel.
     *
     * @throws Error if an I/O error occurs
     */
    @Override
    public native void close() throws Error;
}