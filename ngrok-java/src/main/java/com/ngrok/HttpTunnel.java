package com.ngrok;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An abstract representation of a tunnel connection over HTTP.
 */
public abstract class HttpTunnel extends AgentTunnel {
    /**
     * Constructs a new HTTP tunnel with the specified ID, forwarding address,
     * metadata, protocol, and URL.
     *
     * @param id         the ID of the tunnel
     * @param forwardsTo the forwarding address of the tunnel
     * @param metadata   the metadata of the tunnel
     * @param proto      the protocol of the tunnel
     * @param url        the URL of the tunnel
     */
    public HttpTunnel(String id, String forwardsTo, String metadata, String proto, String url) {
        super(id, forwardsTo, metadata, proto, url);
    }

    /**
     * An enum representing the scheme of an HTTP tunnel.
     */
    public enum Scheme {
        HTTP("HTTP"),
        HTTPS("HTTPS");

        /**
         * The name of the scheme.
         */
        public final String name;

        /**
         * Constructs a new scheme with the specified name.
         *
         * @param name the name of the scheme
         */
        private Scheme(String name) {
            this.name = name;
        }
    }

    /**
     * A class representing an HTTP header.
     */
    public static class Header {
        /**
         * The name of the header.
         */
        private final String name;

        /**
         * The value of the header.
         */
        private final String value;

        /**
         * Constructs a new header with the specified name and value.
         *
         * @param name  the name of the header
         * @param value the value of the header
         */
        public Header(String name, String value) {
            this.name = Objects.requireNonNull(name);
            this.value = Objects.requireNonNull(value);
        }

        /**
         * Returns the name of the header.
         *
         * @return the name of the header
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the value of the header.
         *
         * @return the value of the header
         */
        public String getValue() {
            return value;
        }
    }

    /**
     * A class representing Basic authentication options for an HTTP tunnel.
     */
    public static class BasicAuthOptions {
        /**
         * The username for the authentication.
         */
        private final String username;

        /**
         * The password for the authentication.
         */
        private final String password;

        /**
         * Constructs a new set of basic authentication options with the specified
         * username and password.
         *
         * @param username the username for the authentication
         * @param password the password for the authentication
         */
        public BasicAuthOptions(String username, String password) {
            this.username = Objects.requireNonNull(username);
            this.password = Objects.requireNonNull(password);
        }

        /**
         * Returns the username for the authentication.
         *
         * @return the username for the authentication
         */
        public String getUsername() {
            return username;
        }

        /**
         * Returns the password for the authentication.
         *
         * @return the password for the authentication
         */
        public String getPassword() {
            return password;
        }
    }

    /**
     * A class representing OAuth options for an HTTP tunnel.
     */
    public static class OAuthOptions {
        /**
         * The provider of the OAuth options.
         */
        private final String provider;

        /**
         * The client ID for the OAuth options.
         */
        private String clientId;

        /**
         * The client secret for the OAuth options.
         */
        private String clientSecret;

        /**
         * The email address allowed by the OAuth options.
         */
        private String allowEmail;

        /**
         * The domain allowed by the OAuth options.
         */
        private String allowDomain;

        /**
         * The scope of the OAuth options.
         */
        private String scope;

        /**
         * Constructs a new set of OAuth options with the specified provider.
         *
         * @param provider the provider of the OAuth options
         */
        public OAuthOptions(String provider) {
            this.provider = Objects.requireNonNull(provider);
        }

        /**
         * Sets the client ID and client secret for the OAuth options.
         *
         * @param id     the client ID for the OAuth options
         * @param secret the client secret for the OAuth options
         * @return this OAuthOptions object
         */
        public OAuthOptions client(String id, String secret) {
            this.clientId = Objects.requireNonNull(id);
            this.clientSecret = Objects.requireNonNull(secret);
            return this;
        }

        /**
         * Sets the email address allowed by the OAuth options.
         *
         * @param email the email address allowed by the OAuth options
         * @return this OAuthOptions object
         */
        public OAuthOptions allowEmail(String email) {
            this.allowEmail = Objects.requireNonNull(email);
            return this;
        }

        /**
         * Sets the domain allowed by the OAuth options.
         *
         * @param domain the domain allowed by the OAuth options
         * @return this OAuthOptions object
         */
        public OAuthOptions allowDomain(String domain) {
            this.allowDomain = Objects.requireNonNull(domain);
            return this;
        }

        /**
         * Sets the scope of the OAuth options.
         *
         * @param scope the scope of the OAuth options
         * @return this OAuthOptions object
         */
        public OAuthOptions scope(String scope) {
            this.scope = Objects.requireNonNull(scope);
            return this;
        }

        /**
         * Returns the provider of the OAuth options.
         *
         * @return the provider of the OAuth options
         */
        public String getProvider() {
            return provider;
        }

        /**
         * Returns whether the OAuth options have a client ID.
         *
         * @return true if the OAuth options have a client ID, false otherwise
         */
        public boolean hasClientId() {
            return clientId != null;
        }

        /**
         * Returns the client ID for the OAuth options.
         *
         * @return the client ID for the OAuth options
         */
        public String getClientId() {
            return clientId;
        }

        /**
         * Returns whether the OAuth options have a client secret.
         *
         * @return true if the OAuth options have a client secret, false otherwise
         */
        public boolean hasClientSecret() {
            return clientSecret != null;
        }

        /**
         * Returns the client secret for the OAuth options.
         *
         * @return the client secret for the OAuth options
         */
        public String getClientSecret() {
            return clientSecret;
        }

        /**
         * Returns whether the OAuth options have an allowed email address.
         *
         * @return true if the OAuth options have an allowed email address, false
         *         otherwise
         */
        public boolean hasAllowEmail() {
            return allowEmail != null;
        }

        /**
         * Returns the email address allowed by the OAuth options.
         *
         * @return the email address allowed by the OAuth options
         */
        public String getAllowEmail() {
            return allowEmail;
        }

        /**
         * Returns whether the OAuth options have an allowed domain.
         *
         * @return true if the OAuth options have an allowed domain, false otherwise
         */
        public boolean hasAllowDomain() {
            return allowDomain != null;
        }

        /**
         * Returns the domain allowed by the OAuth options.
         *
         * @return the domain allowed by the OAuth options
         */
        public String getAllowDomain() {
            return allowDomain;
        }

        /**
         * Returns whether the OAuth options have a scope.
         *
         * @return true if the OAuth options have a scope, false otherwise
         */
        public boolean hasScope() {
            return scope != null;
        }

        /**
         * Returns the scope of the OAuth options.
         *
         * @return the scope of the OAuth options
         */
        public String getScope() {
            return scope;
        }
    }

    /**
     * A class representing OIDC options for an HTTP tunnel.
     */
    public static class OIDCOptions {
        /**
         * The issuer URL for the OIDC options.
         */
        private final String issuerUrl;

        /**
         * The client ID for the OIDC options.
         */
        private final String clientId;

        /**
         * The client secret for the OIDC options.
         */
        private final String clientSecret;

        /**
         * The email address allowed by the OIDC options.
         */
        private String allowEmail;

        /**
         * The domain allowed by the OIDC options.
         */
        private String allowDomain;

        /**
         * The scope of the OIDC options.
         */
        private String scope;

        /**
         * Constructs a new set of OIDC options with the specified issuer URL, client
         * ID, and client secret.
         *
         * @param issuerUrl    the issuer URL for the OIDC options
         * @param clientId     the client ID for the OIDC options
         * @param clientSecret the client secret for the OIDC options
         */
        public OIDCOptions(String issuerUrl, String clientId, String clientSecret) {
            this.issuerUrl = Objects.requireNonNull(issuerUrl);
            this.clientId = Objects.requireNonNull(clientId);
            this.clientSecret = Objects.requireNonNull(clientSecret);
        }

        /**
         * Sets the email address allowed by the OIDC options.
         *
         * @param email the email address allowed by the OIDC options
         * @return this OIDCOptions object
         */
        public OIDCOptions allowEmail(String email) {
            this.allowEmail = Objects.requireNonNull(email);
            return this;
        }

        /**
         * Sets the domain allowed by the OIDC options.
         *
         * @param domain the domain allowed by the OIDC options
         * @return this OIDCOptions object
         */
        public OIDCOptions allowDomain(String domain) {
            this.allowDomain = Objects.requireNonNull(domain);
            return this;
        }

        /**
         * Sets the scope of the OIDC options.
         *
         * @param scope the scope of the OIDC options
         * @return this OIDCOptions object
         */
        public OIDCOptions scope(String scope) {
            this.scope = Objects.requireNonNull(scope);
            return this;
        }

        /**
         * Returns the issuer URL for the OIDC options.
         *
         * @return the issuer URL for the OIDC options
         */
        public String getIssuerUrl() {
            return issuerUrl;
        }

        /**
         * Returns the client ID for the OIDC options.
         *
         * @return the client ID for the OIDC options
         */
        public String getClientId() {
            return clientId;
        }

        /**
         * Returns the client secret for the OIDC options.
         *
         * @return the client secret for the OIDC options
         */
        public String getClientSecret() {
            return clientSecret;
        }

        /**
         * Returns whether the OIDC options have an allowed email address.
         *
         * @return true if the OIDC options have an allowed email address, false
         *         otherwise
         */
        public boolean hasAllowEmail() {
            return allowEmail != null;
        }

        /**
         * Returns the email address allowed by the OIDC options.
         *
         * @return the email address allowed by the OIDC options
         */
        public String getAllowEmail() {
            return allowEmail;
        }

        /**
         * Returns whether the OIDC options have an allowed domain.
         *
         * @return true if the OIDC options have an allowed domain, false otherwise
         */
        public boolean hasAllowDomain() {
            return allowDomain != null;
        }

        /**
         * Returns the domain allowed by the OIDC options.
         *
         * @return the domain allowed by the OIDC options
         */
        public String getAllowDomain() {
            return allowDomain;
        }

        /**
         * Returns whether the OIDC options have a scope.
         *
         * @return true if the OIDC options have a scope, false otherwise
         */
        public boolean hasScope() {
            return scope != null;
        }

        /**
         * Returns the scope of the OIDC options.
         *
         * @return the scope of the OIDC options
         */
        public String getScope() {
            return scope;
        }
    }

    /**
     * A class representing webhook verification options for an HTTP tunnel.
     */
    public static class WebhookVerification {
        /**
         * The provider of the webhook verification options.
         */
        private final String provider;

        /**
         * The secret for the webhook verification options.
         */
        private final String secret;

        /**
         * Constructs a new set of webhook verification options with the specified
         * provider and secret.
         *
         * @param provider the provider of the webhook verification options
         * @param secret   the secret for the webhook verification options
         */
        public WebhookVerification(String provider, String secret) {
            this.provider = Objects.requireNonNull(provider);
            this.secret = Objects.requireNonNull(secret);
        }

        /**
         * Returns the provider of the webhook verification options.
         *
         * @return the provider of the webhook verification options
         */
        public String getProvider() {
            return provider;
        }

        /**
         * Returns the secret for the webhook verification options.
         *
         * @return the secret for the webhook verification options
         */
        public String getSecret() {
            return secret;
        }
    }

    /**
     * A builder class for constructing an HTTP tunnel.
     */
    public static class Builder extends AgentTunnel.Builder<Builder> {
        /**
         * The scheme for the HTTP tunnel.
         */
        private Scheme scheme;

        /**
         * The domain for the HTTP tunnel.
         */
        private String domain;

        /**
         * The mutual TLS CA for the HTTP tunnel.
         */
        private byte[] mutualTLSCA;

        /**
         * Whether compression is enabled for the HTTP tunnel.
         */
        private boolean compression = false;

        /**
         * Whether WebSocket to TCP conversion is enabled for the HTTP tunnel.
         */
        private boolean websocketTcpConversion = false;

        /**
         * The circuit breaker value for the HTTP tunnel.
         */
        private Optional<Double> circuitBreaker = Optional.empty();

        /**
         * The request headers for the HTTP tunnel.
         */
        private final List<Header> requestHeaders = new ArrayList<>();

        /**
         * The response headers for the HTTP tunnel.
         */
        private final List<Header> responseHeaders = new ArrayList<>();

        /**
         * The request headers to remove for the HTTP tunnel.
         */
        private final List<String> removeRequestHeaders = new ArrayList<>();

        /**
         * The response headers to remove for the HTTP tunnel.
         */
        private final List<String> removeResponseHeaders = new ArrayList<>();

        /**
         * The basic authentication options for the HTTP tunnel.
         */
        private BasicAuthOptions basicAuthOptions;

        /**
         * The OAuth options for the HTTP tunnel.
         */
        private OAuthOptions oauthOptions;

        /**
         * The OIDC options for the HTTP tunnel.
         */
        private OIDCOptions oidcOptions;

        /**
         * The webhook verification options for the HTTP tunnel.
         */
        private WebhookVerification webhookVerification;

        /**
         * Sets the scheme for the HTTP tunnel.
         *
         * @param scheme the scheme for the HTTP tunnel
         * @return this Builder object
         */
        public Builder scheme(Scheme scheme) {
            this.scheme = scheme;
            return this;
        }

        /**
         * Returns whether the HTTP tunnel has a scheme.
         *
         * @return true if the HTTP tunnel has a scheme, false otherwise
         */
        public boolean hasScheme() {
            return scheme != null;
        }

        /**
         * Returns the name of the scheme for the HTTP tunnel.
         *
         * @return the name of the scheme for the HTTP tunnel
         */
        public String getSchemeName() {
            return scheme.name;
        }

        /**
         * Sets the domain for the HTTP tunnel.
         *
         * @param domain the domain for the HTTP tunnel
         * @return this Builder object
         */
        public Builder domain(String domain) {
            this.domain = Objects.requireNonNull(domain);
            return this;
        }

        /**
         * Returns whether the HTTP tunnel has a domain.
         *
         * @return true if the HTTP tunnel has a domain, false otherwise
         */
        public boolean hasDomain() {
            return domain != null;
        }

        /**
         * Returns the domain for the HTTP tunnel.
         *
         * @return the domain for the HTTP tunnel
         */
        public String getDomain() {
            return domain;
        }

        /**
         * Sets the mutual TLS CA for the HTTP tunnel.
         *
         * @param mutualTLSCA the mutual TLS CA for the HTTP tunnel
         * @return this Builder object
         */
        public Builder mutualTLSCA(byte[] mutualTLSCA) {
            this.mutualTLSCA = Objects.requireNonNull(mutualTLSCA);
            return this;
        }

        /**
         * Returns the mutual TLS CA for the HTTP tunnel.
         *
         * @return the mutual TLS CA for the HTTP tunnel
         */
        public byte[] getMutualTLSCA() {
            return mutualTLSCA;
        }

        /**
         * Enables compression for the HTTP tunnel.
         *
         * @return this Builder object
         */
        public Builder compression() {
            this.compression = true;
            return this;
        }

        /**
         * Returns whether compression is enabled for the HTTP tunnel.
         *
         * @return true if compression is enabled for the HTTP tunnel, false otherwise
         */
        public boolean isCompression() {
            return compression;
        }

        /**
         * Enables WebSocket to TCP conversion for the HTTP tunnel.
         *
         * @return this Builder object
         */
        public Builder websocketTcpConversion() {
            this.websocketTcpConversion = true;
            return this;
        }

        /**
         * Returns whether WebSocket to TCP conversion is enabled for the HTTP tunnel.
         *
         * @return true if WebSocket to TCP conversion is enabled for the HTTP tunnel,
         *         false otherwise
         */
        public boolean isWebsocketTcpConversion() {
            return websocketTcpConversion;
        }

        /**
         * Sets the circuit breaker value for the HTTP tunnel.
         *
         * @param value the circuit breaker value for the HTTP tunnel
         * @return this Builder object
         */
        public Builder circuitBreaker(double value) {
            this.circuitBreaker = Optional.of(value);
            return this;
        }

        /**
         * Returns whether the HTTP tunnel has a circuit breaker value.
         *
         * @return true if the HTTP tunnel has a circuit breaker value, false otherwise
         */
        public boolean hasCircuitBreaker() {
            return circuitBreaker.isPresent();
        }

        /**
         * Returns the circuit breaker value for the HTTP tunnel.
         *
         * @return the circuit breaker value for the HTTP tunnel
         */
        public double getCircuitBreaker() {
            return circuitBreaker.get();
        }

        /**
         * Adds a request header to the HTTP tunnel.
         *
         * @param name  the name of the request header to add
         * @param value the value of the request header to add
         * @return this Builder object
         */
        public Builder addRequestHeader(String name, String value) {
            this.requestHeaders.add(new Header(name, value));
            return this;
        }

        /**
         * Returns the request headers for the HTTP tunnel.
         *
         * @return the request headers for the HTTP tunnel
         */
        public List<Header> getRequestHeaders() {
            return requestHeaders;
        }

        /**
         * Adds a response header to the HTTP tunnel.
         *
         * @param name  the name of the response header to add
         * @param value the value of the response header to add
         * @return this Builder object
         */
        public Builder addResponseHeader(String name, String value) {
            this.responseHeaders.add(new Header(name, value));
            return this;
        }

        /**
         * Returns the response headers for the HTTP tunnel.
         *
         * @return the response headers for the HTTP tunnel
         */
        public List<Header> getResponseHeaders() {
            return responseHeaders;
        }

        /**
         * Adds a request header to remove from the HTTP tunnel.
         *
         * @param name the name of the request header to remove
         * @return this Builder object
         */
        public Builder removeRequestHeader(String name) {
            this.removeRequestHeaders.add(Objects.requireNonNull(name));
            return this;
        }

        /**
         * Returns the request headers to remove from the HTTP tunnel.
         *
         * @return the request headers to remove from the HTTP tunnel
         */
        public List<String> getRemoveRequestHeaders() {
            return removeRequestHeaders;
        }

        /**
         * Adds a response header to remove from the HTTP tunnel.
         *
         * @param name the name of the response header to remove
         * @return this Builder object
         */
        public Builder removeResponseHeader(String name) {
            this.removeResponseHeaders.add(Objects.requireNonNull(name));
            return this;
        }

        /**
         * Returns the response headers to remove from the HTTP tunnel.
         *
         * @return the response headers to remove from the HTTP tunnel
         */
        public List<String> getRemoveResponseHeaders() {
            return removeResponseHeaders;
        }

        /**
         * Sets the basic authentication options for the HTTP tunnel.
         *
         * @param options the basic authentication options for the HTTP tunnel
         * @return this Builder object
         */
        public Builder basicAuthOptions(BasicAuthOptions options) {
            this.basicAuthOptions = options;
            return this;
        }

        /**
         * Sets the OAuth options for the HTTP tunnel.
         *
         * @param options the OAuth options for the HTTP tunnel
         * @return this Builder object
         */
        public Builder oauthOptions(OAuthOptions options) {
            this.oauthOptions = options;
            return this;
        }

        /**
         * Sets the OIDC options for the HTTP tunnel.
         *
         * @param options the OIDC options for the HTTP tunnel
         * @return this Builder object
         */
        public Builder oidcOptions(OIDCOptions options) {
            this.oidcOptions = options;
            return this;
        }

        /**
         * Sets the webhook verification options for the HTTP tunnel.
         *
         * @param webhookVerification the webhook verification options for the HTTP
         *                            tunnel
         * @return this Builder object
         */
        public Builder webhookVerification(WebhookVerification webhookVerification) {
            this.webhookVerification = webhookVerification;
            return this;
        }

        /**
         * Returns the basic authentication options for the HTTP tunnel.
         *
         * @return the basic authentication options for the HTTP tunnel
         */
        public BasicAuthOptions getBasicAuthOptions() {
            return basicAuthOptions;
        }

        /**
         * Returns the OAuth options for the HTTP tunnel.
         *
         * @return the OAuth options for the HTTP tunnel
         */
        public OAuthOptions getOauthOptions() {
            return oauthOptions;
        }

        /**
         * Returns the OIDC options for the HTTP tunnel.
         *
         * @return the OIDC options for the HTTP tunnel
         */
        public OIDCOptions getOidcOptions() {
            return oidcOptions;
        }

        /**
         * Returns the webhook verification options for the HTTP tunnel.
         *
         * @return the webhook verification options for the HTTP tunnel
         */
        public WebhookVerification getWebhookVerification() {
            return webhookVerification;
        }
    }
}
