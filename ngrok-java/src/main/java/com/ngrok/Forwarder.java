package com.ngrok;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public interface Forwarder extends AutoCloseable {
    String getId();

    String getMetadata();

    String getForwardsTo();

    void join() throws IOException;

    @Override
    void close() throws IOException;

    interface Builder<F extends Forwarder> {
        F forward(URL url) throws IOException;
    }

    interface Endpoint extends Forwarder {
        String getProto();

        String getUrl();
    }

    interface Edge extends Forwarder {
        Map<String, String> getLabels();
    }
}
