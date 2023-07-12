package com.ngrok.net;

import com.ngrok.Connection;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;

public class ConnectionOutputStreamTest {
    @Test
    public void testStreamChunking() throws Exception {
        var conn = new CollectingConnection();
        var os = new ConnectionOutputStream(conn, 8);

        // an array of 32 bytes
        var data = "0123456789 0123456789 0123456789".getBytes(StandardCharsets.UTF_8);
        assertEquals(32, data.length);

        for (int low = 0; low < data.length; low++) {
            for (int high = low; high < data.length; high++) {
                os.write(data, low, high-low);
                conn.data.flip();

                assertEquals(high-low, conn.data.limit());
                for (int k = low; k < high; k++) {
                    assertEquals(data[k], conn.data.get());
                }
                conn.data.clear();
            }
        }
    }

    private static class CollectingConnection extends Connection {
        private final ByteBuffer data = ByteBuffer.allocate(1024);
        CollectingConnection() {
            super("local");
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            return 0;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            data.put(src);
            return src.limit();
        }

        @Override
        public void close() throws IOException {

        }
    }
}
