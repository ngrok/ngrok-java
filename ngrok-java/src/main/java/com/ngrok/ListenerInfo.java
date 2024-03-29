package com.ngrok;

import java.util.Map;

/**
 * Represents information about a running {@link Listener}.
 */
public interface ListenerInfo {
    /**
     * Returns the id associated with this listener
     *
     * @return the id
     */
    String getId();

    /**
     * Returns the metadata associated with this listener
     *
     * @return the metadata
     */
    String getMetadata();

    /**
     * Returns the target of that listener
     *
     * @return the target
     */
    String getForwardsTo();

    /**
     * Represents information about a running {@link Listener.Endpoint}.
     */
    interface Endpoint extends ListenerInfo {
        /**
         * Returns the protocol associated with this listener.
         *
         * @return the protocol, for example {@code http} or {@code tcp}
         */
        String getProto();

        /**
         * Returns the URL at which this listener receives new connections.
         *
         * @return the url
         */
        String getUrl();
    }

    /**
     * Represents information about a running {@link Listener.Edge}.
     */
    interface Edge extends ListenerInfo {
        /**
         * Returns the labels associated with this listener.
         *
         * @return the labels
         */
        Map<String, String> getLabels();
    }
}
