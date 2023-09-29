package com.ngrok;

import java.util.Map;

public interface ListenerInfo {
    String getId();

    String getMetadata();

    String getForwardsTo();

    interface Endpoint extends ListenerInfo {
        String getProto();

        String getUrl();
    }

    interface Edge extends ListenerInfo {
        Map<String, String> getLabels();
    }
}
