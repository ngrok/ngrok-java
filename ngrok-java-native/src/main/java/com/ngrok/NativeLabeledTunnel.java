package com.ngrok;

import java.io.IOException;

/**
* An implementation of {@link LabeledTunnel} that delegates calls to a native library.
 */
public class NativeLabeledTunnel extends LabeledTunnel {
    private long native_address;

    /**
     * Constructs a new {@link NativeLabeledTunnel} with the specified ID, forwarding address, and metadata.
     *
     * @param id the ID of the tunnel
     * @param forwardsTo the forwarding address of the tunnel
     * @param metadata the metadata of the tunnel
     */
    public NativeLabeledTunnel(String id, String forwardsTo, String metadata) {
        super(id, forwardsTo, metadata);
    }

    /**
     * Accepts a new connection on the labeled tunnel.
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
     * Closes the labeled tunnel.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public native void close() throws IOException;
}
