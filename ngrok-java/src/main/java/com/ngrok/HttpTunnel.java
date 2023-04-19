package com.ngrok;

import java.util.ArrayList;
import java.util.List;

public abstract class HttpTunnel extends AgentTunnel {
    public static class Header {
        public final String name;
        public final String value;

        public Header(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String name() {
            return name;
        }

        public String value() {
            return value;
        }
    }

    public static class BasicAuthOptions {
        public final String username;
        public final String password;

        public BasicAuthOptions(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String username() {
            return username;
        }

        public String password() {
            return password;
        }
    }

    public static class Builder extends AgentTunnel.Builder<Builder> {
        public String domain;

        public byte[] mutualTLSCA;

        public final List<Header> requestHeaders = new ArrayList<>();
        public final List<Header> responseHeaders = new ArrayList<>();
        public final List<String> removeRequestHeaders = new ArrayList<>();
        public final List<String> removeResponseHeaders = new ArrayList<>();

        public BasicAuthOptions basicAuthOptions;

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder mutualTLSCA(byte[] mutualTLSCA) {
            this.mutualTLSCA = mutualTLSCA;
            return this;
        }

        public byte[] mutualTLSCA() {
            return mutualTLSCA;
        }

        public Builder addRequestHeader(String name, String value) {
            this.requestHeaders.add(new Header(name, value));
            return this;
        }

        public Builder addResponseHeader(String name, String value) {
            this.responseHeaders.add(new Header(name, value));
            return this;
        }

        public Builder removeRequestHeader(String name) {
            this.removeRequestHeaders.add(name);
            return this;
        }

        public Builder removeResponseHeader(String name) {
            this.removeResponseHeaders.add(name);
            return this;
        }

        public Builder basicAuthOptions(BasicAuthOptions options) {
            this.basicAuthOptions = options;
            return this;
        }

        public BasicAuthOptions basicAuthOptions() {
            return basicAuthOptions;
        }
    }
}
