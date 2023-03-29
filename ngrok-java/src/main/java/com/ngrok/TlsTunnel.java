package com.ngrok;

import java.io.IOException;

public abstract class TlsTunnel extends AgentTunnel {
    public static class Builder extends AgentTunnel.Builder {
        public String domain;

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }
    }
}
