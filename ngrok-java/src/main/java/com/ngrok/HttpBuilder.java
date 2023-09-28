package com.ngrok;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A builder class for constructing an HTTP tunnel.
 */
public class HttpBuilder extends EndpointBuilder<HttpBuilder>
        implements Listener.Builder<Listener.Endpoint>, Forwarder.Builder<Forwarder.Endpoint> {
    private final Session session;

    /**
     * The scheme for the HTTP tunnel.
     */
    private Http.Scheme scheme;

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
    private final List<Http.Header> requestHeaders = new ArrayList<>();

    /**
     * The response headers for the HTTP tunnel.
     */
    private final List<Http.Header> responseHeaders = new ArrayList<>();

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
    private Http.BasicAuth basicAuthOptions;

    /**
     * The OAuth options for the HTTP tunnel.
     */
    private Http.OAuth oauthOptions;

    /**
     * The OIDC options for the HTTP tunnel.
     */
    private Http.OIDC oidcOptions;

    /**
     * The webhook verification options for the HTTP tunnel.
     */
    private Http.WebhookVerification webhookVerification;

    public HttpBuilder(Session session) {
        this.session = session;
    }

    /**
     * Sets the scheme for the HTTP tunnel.
     *
     * @param scheme the scheme for the HTTP tunnel
     * @return this Builder object
     */
    public HttpBuilder scheme(Http.Scheme scheme) {
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
    public HttpBuilder domain(String domain) {
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
    public HttpBuilder mutualTLSCA(byte[] mutualTLSCA) {
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
    public HttpBuilder compression() {
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
    public HttpBuilder websocketTcpConversion() {
        this.websocketTcpConversion = true;
        return this;
    }

    /**
     * Returns whether WebSocket to TCP conversion is enabled for the HTTP tunnel.
     *
     * @return true if WebSocket to TCP conversion is enabled for the HTTP tunnel,
     * false otherwise
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
    public HttpBuilder circuitBreaker(double value) {
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
    public HttpBuilder addRequestHeader(String name, String value) {
        this.requestHeaders.add(new Http.Header(name, value));
        return this;
    }

    /**
     * Returns the request headers for the HTTP tunnel.
     *
     * @return the request headers for the HTTP tunnel
     */
    public List<Http.Header> getRequestHeaders() {
        return requestHeaders;
    }

    /**
     * Adds a response header to the HTTP tunnel.
     *
     * @param name  the name of the response header to add
     * @param value the value of the response header to add
     * @return this Builder object
     */
    public HttpBuilder addResponseHeader(String name, String value) {
        this.responseHeaders.add(new Http.Header(name, value));
        return this;
    }

    /**
     * Returns the response headers for the HTTP tunnel.
     *
     * @return the response headers for the HTTP tunnel
     */
    public List<Http.Header> getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * Adds a request header to remove from the HTTP tunnel.
     *
     * @param name the name of the request header to remove
     * @return this Builder object
     */
    public HttpBuilder removeRequestHeader(String name) {
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
    public HttpBuilder removeResponseHeader(String name) {
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
    public HttpBuilder basicAuthOptions(Http.BasicAuth options) {
        this.basicAuthOptions = options;
        return this;
    }

    /**
     * Sets the OAuth options for the HTTP tunnel.
     *
     * @param options the OAuth options for the HTTP tunnel
     * @return this Builder object
     */
    public HttpBuilder oauthOptions(Http.OAuth options) {
        this.oauthOptions = options;
        return this;
    }

    /**
     * Sets the OIDC options for the HTTP tunnel.
     *
     * @param options the OIDC options for the HTTP tunnel
     * @return this Builder object
     */
    public HttpBuilder oidcOptions(Http.OIDC options) {
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
    public MetadataBuilder webhookVerification(Http.WebhookVerification webhookVerification) {
        this.webhookVerification = webhookVerification;
        return this;
    }

    /**
     * Returns the basic authentication options for the HTTP tunnel.
     *
     * @return the basic authentication options for the HTTP tunnel
     */
    public Http.BasicAuth getBasicAuth() {
        return basicAuthOptions;
    }

    /**
     * Returns the OAuth options for the HTTP tunnel.
     *
     * @return the OAuth options for the HTTP tunnel
     */
    public Http.OAuth getOauth() {
        return oauthOptions;
    }

    /**
     * Returns the OIDC options for the HTTP tunnel.
     *
     * @return the OIDC options for the HTTP tunnel
     */
    public Http.OIDC getOidc() {
        return oidcOptions;
    }

    /**
     * Returns the webhook verification options for the HTTP tunnel.
     *
     * @return the webhook verification options for the HTTP tunnel
     */
    public Http.WebhookVerification getWebhookVerification() {
        return webhookVerification;
    }

    @Override
    public Listener.Endpoint listen() throws IOException {
        return session.listenHttp(this);
    }

    @Override
    public Forwarder.Endpoint forward(URL url) throws IOException {
        return session.forwardHttp(this, url);
    }
}
