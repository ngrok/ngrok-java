package com.ngrok;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class HttpTunnel extends AgentTunnel {
    public HttpTunnel(String id, String forwardsTo, String metadata, String proto, String url) {
        super(id, forwardsTo, metadata, proto, url);
    }

    public enum Scheme {
        HTTP("HTTP"),
        HTTPS("HTTPS");

        public final String name;

        private Scheme(String name) {
            this.name = name;
        }
    }

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

    public static class OAuthOptions {
        private final String provider;

        private String clientId;
        private String clientSecret;

        private String allowEmail;
        private String allowDomain;
        private String scope;

        public OAuthOptions(String provider) {
            this.provider = Objects.requireNonNull(provider);
        }

        public OAuthOptions client(String id, String secret) {
            this.clientId = Objects.requireNonNull(id);
            this.clientSecret = Objects.requireNonNull(secret);
            return this;
        }

        public OAuthOptions allowEmail(String email) {
            this.allowEmail = Objects.requireNonNull(email);
            return this;
        }

        public OAuthOptions allowDomain(String domain) {
            this.allowDomain = Objects.requireNonNull(domain);
            return this;
        }

        public OAuthOptions scope(String scope) {
            this.scope = Objects.requireNonNull(scope);
            return this;
        }

        public String getProvider() {
            return provider;
        }

        public boolean hasClientId() {
            return clientId != null;
        }

        public String getClientId() {
            return clientId;
        }

        public boolean hasClientSecret() {
            return clientSecret != null;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public boolean hasAllowEmail() {
            return allowEmail != null;
        }

        public String getAllowEmail() {
            return allowEmail;
        }

        public boolean hasAllowDomain() {
            return allowDomain != null;
        }

        public String getAllowDomain() {
            return allowDomain;
        }

        public boolean hasScope() {
            return scope != null;
        }

        public String getScope() {
            return scope;
        }
    }

    public static class OIDCOptions {
        private final String issuerUrl;
        private final String clientId;
        private final String clientSecret;

        private String allowEmail;
        private String allowDomain;
        private String scope;

        public OIDCOptions(String issuerUrl, String clientId, String clientSecret) {
            this.issuerUrl = Objects.requireNonNull(issuerUrl);
            this.clientId = Objects.requireNonNull(clientId);
            this.clientSecret = Objects.requireNonNull(clientSecret);
        }

        public OIDCOptions allowEmail(String email) {
            this.allowEmail = Objects.requireNonNull(email);
            return this;
        }

        public OIDCOptions allowDomain(String domain) {
            this.allowDomain = Objects.requireNonNull(domain);
            return this;
        }

        public OIDCOptions scope(String scope) {
            this.scope = Objects.requireNonNull(scope);
            return this;
        }

        public String getIssuerUrl() {
            return issuerUrl;
        }

        public String getClientId() {
            return clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public boolean hasAllowEmail() {
            return allowEmail != null;
        }

        public String getAllowEmail() {
            return allowEmail;
        }

        public boolean hasAllowDomain() {
            return allowDomain != null;
        }

        public String getAllowDomain() {
            return allowDomain;
        }

        public boolean hasScope() {
            return scope != null;
        }

        public String getScope() {
            return scope;
        }
    }

    public static class WebhookVerification {
        private final String provider;
        private final String secret;

        public WebhookVerification(String provider, String secret) {
            this.provider = Objects.requireNonNull(provider);
            this.secret = Objects.requireNonNull(secret);
        }

        public String getProvider() {
            return provider;
        }

        public String getSecret() {
            return secret;
        }
    }

    public static class Builder extends AgentTunnel.Builder<Builder> {
        private Scheme scheme;

        private String domain;

        private byte[] mutualTLSCA;

        private boolean compression = false;
        private boolean websocketTcpConversion = false;
        private Optional<Double> circuitBreaker = Optional.empty();

        private final List<Header> requestHeaders = new ArrayList<>();
        private final List<Header> responseHeaders = new ArrayList<>();
        private final List<String> removeRequestHeaders = new ArrayList<>();
        private final List<String> removeResponseHeaders = new ArrayList<>();

        private BasicAuthOptions basicAuthOptions;
        private OAuthOptions oauthOptions;
        private OIDCOptions oidcOptions;
        private WebhookVerification webhookVerification;

        public Builder scheme(Scheme scheme) {
            this.scheme = scheme;
            return this;
        }

        public boolean hasScheme() {
            return scheme != null;
        }

        public String getSchemeName() {
            return scheme.name;
        }

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

        public Builder compression() {
            this.compression = true;
            return this;
        }

        public boolean isCompression() {
            return compression;
        }

        public Builder websocketTcpConversion() {
            this.websocketTcpConversion = true;
            return this;
        }

        public boolean isWebsocketTcpConversion() {
            return websocketTcpConversion;
        }

        public Builder circuitBreaker(double value) {
            this.circuitBreaker = Optional.of(value);
            return this;
        }

        public boolean hasCircuitBreaker() {
            return circuitBreaker.isPresent();
        }

        public double getCircuitBreaker() {
            return circuitBreaker.get();
        }

        public Builder addRequestHeader(String name, String value) {
            this.requestHeaders.add(new Header(name, value));
            return this;
        }

        public List<Header> getRequestHeaders() {
            return requestHeaders;
        }

        public Builder addResponseHeader(String name, String value) {
            this.responseHeaders.add(new Header(name, value));
            return this;
        }

        public List<Header> getResponseHeaders() {
            return responseHeaders;
        }

        public Builder removeRequestHeader(String name) {
            this.removeRequestHeaders.add(Objects.requireNonNull(name));
            return this;
        }

        public List<String> getRemoveRequestHeaders() {
            return removeRequestHeaders;
        }

        public Builder removeResponseHeader(String name) {
            this.removeResponseHeaders.add(Objects.requireNonNull(name));
            return this;
        }

        public List<String> getRemoveResponseHeaders() {
            return removeResponseHeaders;
        }

        public Builder basicAuthOptions(BasicAuthOptions options) {
            this.basicAuthOptions = options;
            return this;
        }

        public Builder oauthOptions(OAuthOptions options) {
            this.oauthOptions = options;
            return this;
        }

        public Builder oidcOptions(OIDCOptions options) {
            this.oidcOptions = options;
            return this;
        }

        public Builder webhookVerification(WebhookVerification webhookVerification) {
            this.webhookVerification = webhookVerification;
            return this;
        }

        public BasicAuthOptions getBasicAuthOptions() {
            return basicAuthOptions;
        }

        public OAuthOptions getOauthOptions() {
            return oauthOptions;
        }

        public OIDCOptions getOidcOptions() {
            return oidcOptions;
        }

        public WebhookVerification getWebhookVerification() {
            return webhookVerification;
        }
    }
}
