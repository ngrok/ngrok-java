package com.ngrok.jetty;

import com.ngrok.Connection;

import org.eclipse.jetty.io.AbstractEndPoint;
import org.eclipse.jetty.util.thread.Scheduler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class NgrokEndpoint extends AbstractEndPoint {
    private final Connection conn;

    public NgrokEndpoint(Scheduler scheduler, Connection conn) {
        super(scheduler);
        this.conn = conn;

        onOpen();
    }

    @Override
    protected void onIncompleteFlush() {
        throw new UnsupportedOperationException("noful");
    }

    @Override
    protected void needsFillInterest() throws IOException {
        getScheduler().schedule(() -> getFillInterest().fillable(), 0, TimeUnit.NANOSECONDS);
    }

    @Override
    public int fill(ByteBuffer buffer) throws IOException {
        return conn.read(buffer);
    }

    @Override
    public boolean flush(ByteBuffer... buffer) throws IOException {
        for (var b : buffer){
            int sz = conn.write(b);
            if (sz < b.limit()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object getTransport() {
        throw new UnsupportedOperationException("ohnoe");
    }
}
