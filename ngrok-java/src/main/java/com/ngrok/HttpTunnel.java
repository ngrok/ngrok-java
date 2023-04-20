package com.ngrok;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class HttpTunnel extends AgentTunnel {
    public static class Header {
        private final String name;
        private final String value;

        public Header(String name, String value) {
            this.name = Objects.requireNonNull(name);
            this.value = Objects.requireNonNull(value);
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    public static class BasicAuthOptions {
        private final String username;
        private final String password;

        public BasicAuthOptions(String username, String password) {
            this.username = Objects.requireNonNull(username);
            this.password = Objects.requireNonNull(password);
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    public static class Builder extends AgentTunnel.Builder<Builder> {
        private String domain;

        private byte[] mutualTLSCA;

        public final List<Header> requestHeaders = new ArrayList<>();
        public final List<Header> responseHeaders = new ArrayList<>();
        public final List<String> removeRequestHeaders = new ArrayList<>();
        public final List<String> removeResponseHeaders = new ArrayList<>();

        public BasicAuthOptions basicAuthOptions;

        public Builder domain(String domain) {
            this.domain = Objects.requireNonNull(domain);
            return this;
        }

        public boolean hasDomain() {
            return domain != null;
        }

        public String getDomain() {
            return domain;
        }

        public Builder mutualTLSCA(byte[] mutualTLSCA) {
            this.mutualTLSCA = Objects.requireNonNull(mutualTLSCA);
            return this;
        }

        public byte[] getMutualTLSCA() {
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
            this.removeRequestHeaders.add(Objects.requireNonNull(name));
            return this;
        }

        public Builder removeResponseHeader(String name) {
            this.removeResponseHeaders.add(Objects.requireNonNull(name));
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
