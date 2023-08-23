package com.ngrok;


/**
* An implementation of {@link TlsTunnel} that delegates calls to a native library.
 */
public class NativeTlsTunnel extends TlsTunnel {
    private long native_address;

    /**
     * Constructs a new TLS tunnel with the specified ID, forwarding address,
     * metadata, protocol, and URL.
     *
     * @param id         the ID of the tunnel
     * @param forwardsTo the forwarding address of the tunnel
     * @param metadata   the metadata of the tunnel
     * @param proto      the protocol of the tunnel
     * @param url        the URL of the tunnel
     */
    public NativeTlsTunnel(String id, String forwardsTo, String metadata, String proto, String url) {
        super(id, forwardsTo, metadata, proto, url);
    }

    /**
     * Accepts a new connection on the TLS tunnel.
     *
     * @return a new NativeConnection object representing the accepted connection
     * @throws Error if an I/O error occurs
     */
    @Override
    public native NativeConnection accept() throws Error;

    /**
     * Forwards TCP traffic to the specified address.
     *
     * @param addr the address to forward TCP traffic to
     * @throws Error if an I/O error occurs
     */
    @Override
    public native void forwardTcp(String addr) throws Error;

    /**
     * Closes the TLS tunnel.
     *
     * @throws Error if an I/O error occurs
     */
    @Override
    public native void close() throws Error;
}
