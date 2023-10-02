package com.ngrok.jetty;

import com.ngrok.Connection;

import org.eclipse.jetty.io.AbstractEndPoint;
import org.eclipse.jetty.util.thread.Scheduler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * A class representing an endpoint for ngrok connection.
 */
public class NgrokEndpoint extends AbstractEndPoint {
    private final Connection conn;

    /**
     * Constructs a new ngrok endpoint with the specified scheduler and connection.
     *
     * @param scheduler the scheduler to use for the endpoint
     * @param conn      the connection to use for the endpoint
     */
    public NgrokEndpoint(Scheduler scheduler, Connection conn) {
        super(scheduler);
        this.conn = conn;

        onOpen();
    }

    /**
     * Throws an {@link UnsupportedOperationException}, as incomplete flush is not supported
     * by ngrok endpoints.
     *
     * @throws UnsupportedOperationException if the method is called
     */
    @Override
    protected void onIncompleteFlush() {
        throw new UnsupportedOperationException("noful");
    }

    /**
     * Schedules the endpoint to be filled with interest.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void needsFillInterest() throws IOException {
        getScheduler().schedule(() -> getFillInterest().fillable(), 0, TimeUnit.NANOSECONDS);
    }

    /**
     * Fills the endpoint with data from the connection.
     *
     * @param buffer the buffer to fill with data
     * @return the number of bytes read from the connection
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int fill(ByteBuffer buffer) throws IOException {
        return conn.read(buffer);
    }

    /**
     * Flushes the endpoint with data from the connection.
     *
     * @param buffer the buffer to flush with data
     * @return true if the buffer was completely flushed, false otherwise
     * @throws IOException if an I/O error occurs
     */
    @Override
    public boolean flush(ByteBuffer... buffer) throws IOException {
        for (var b : buffer) {
            int sz = conn.write(b);
            if (sz < b.limit()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Throws an {@link UnsupportedOperationException}, as the transport used by ngrok
     * endpoints is not supported.
     *
     * @throws UnsupportedOperationException if the method is called
     */
    @Override
    public Object getTransport() {
        throw new UnsupportedOperationException("ohnoe");
    }
}
