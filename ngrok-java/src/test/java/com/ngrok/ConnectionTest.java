package com.ngrok;

import com.ngrok.Connection;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConnectionTest {
    @Test
    public void testInetAddress() throws Exception {
        var conn = new ConnectionAddressMock();

        conn.remoteAddr = "4.3.2.1:8000";
        var addr = conn.inetAddress();
        assertNotNull(addr);
        assertEquals("4.3.2.1", addr.getHostName());
        assertEquals(8000, addr.getPort());

        conn.remoteAddr = "[2a00:23c8:a8dd:e501:b02c:ea1a:83cf:395e]:64440";
        addr = conn.inetAddress();
        assertNotNull(addr);
        assertEquals("2a00:23c8:a8dd:e501:b02c:ea1a:83cf:395e", addr.getHostName());
        assertEquals(64440, addr.getPort());
    }

    class ConnectionAddressMock implements Connection {
        String remoteAddr;

        @Override
        public String getRemoteAddr() {
            return remoteAddr;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
