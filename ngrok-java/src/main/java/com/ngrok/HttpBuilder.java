package com.ngrok;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A builder for creating a HTTP endpoint listener
 */
public class HttpBuilder extends EndpointBuilder<HttpBuilder>
        implements Listener.Builder<Listener.Endpoint>, Forwarder.Builder<Forwarder.Endpoint> {
    private final Session session;

    private Http.Scheme scheme;
    private Optional<String> domain = Optional.empty();
    private byte[] mutualTLSCA;
    private boolean compression = false;
    private boolean websocketTcpConversion = false;
    private Optional<Double> circuitBreaker = Optional.empty();
    private final List<Http.Header> requestHeaders = new ArrayList<>();
    private final List<Http.Header> responseHeaders = new ArrayList<>();
    private final List<String> removeRequestHeaders = new ArrayList<>();
    private final List<String> removeResponseHeaders = new ArrayList<>();
    private Http.BasicAuth basicAuthOptions;
    private Http.OAuth oauthOptions;
    private Http.OIDC oidcOptions;
    private Http.WebhookVerification webhookVerification;

    /**
     * Creates a new {@link HttpBuilder} with a given session.
     *
     * @param session the session over which this listener will connect.
     *                If {@code null}, {@link #listen()} and {@link #forward(URL)}
     *                will throw {@link NullPointerException}, use the corresponding
     *                methods on the {@link Session} object directly.
     */
    public HttpBuilder(Session session) {
        this.session = session;
    }

    /**
     * Sets the scheme for this builder.
     *
     * @param scheme the scheme
     * @return the builder instance
     */
    public HttpBuilder scheme(Http.Scheme scheme) {
        this.scheme = Objects.requireNonNull(scheme);
        return this;
    }

    /**
     * Sets the domain for this builder.
     *
     * @param domain the domain
     * @return the builder instance
     */
    public HttpBuilder domain(String domain) {
        this.domain = Optional.of(domain);
        return this;
    }

    /**
     * Set the mutual TLS certificate authority for this builder.
     *
     * @param mutualTLSCA the TLS certificate authority, in bytes
     * @return the builder instance
     */
    public HttpBuilder mutualTLSCA(byte[] mutualTLSCA) {
        this.mutualTLSCA = Objects.requireNonNull(mutualTLSCA);
        return this;
    }

    /**
     * Enables compression for this builder.
     *
     * @return the builder instance
     */
    public HttpBuilder compression() {
        this.compression = true;
        return this;
    }

    /**
     * Enables WebSocket to TCP conversion for this builder.
     *
     * @return the builder instance
     */
    public HttpBuilder websocketTcpConversion() {
        this.websocketTcpConversion = true;
        return this;
    }

    /**
     * Sets the circuit breaker value for this builder.
     *
     * @param value the circuit breaker value, between 0 and 1
     * @return the builder instance
     */
    public HttpBuilder circuitBreaker(double value) {
        this.circuitBreaker = Optional.of(value);
        return this;
    }

    /**
     * Adds a header to the list of added request headers for this builder.
     *
     * @param name  the name of the header to add
     * @param value the value of the header to add
     * @return the builder instance
     */
    public HttpBuilder addRequestHeader(String name, String value) {
        this.requestHeaders.add(new Http.Header(name, value));
        return this;
    }

    /**
     * Adds a header to the list of added response headers for this builder.
     *
     * @param name  the name of the header to add
     * @param value the value of the header to add
     * @return the builder instance
     */
    public HttpBuilder addResponseHeader(String name, String value) {
        this.responseHeaders.add(new Http.Header(name, value));
        return this;
    }

    /**
     * Adds a header to the list of removed request headers for this builder.
     *
     * @param name the name of the header to remove
     * @return the builder instance
     */
    public HttpBuilder removeRequestHeader(String name) {
        this.removeRequestHeaders.add(Objects.requireNonNull(name));
        return this;
    }

    /**
     * Adds a header to the list of removed response headers for this builder.
     *
     * @param name the name of the header to remove
     * @return the builder instance
     */
    public HttpBuilder removeResponseHeader(String name) {
        this.removeResponseHeaders.add(Objects.requireNonNull(name));
        return this;
    }

    /**
     * Sets basic authentication for this builder.
     *
     * @param options the basic authentication options
     * @return the builder instance
     */
    public HttpBuilder basicAuthOptions(Http.BasicAuth options) {
        this.basicAuthOptions = options;
        return this;
    }

    /**
     * Sets OAuth for this builder.
     *
     * @param options the OAuth options
     * @return the builder instance
     */
    public HttpBuilder oauthOptions(Http.OAuth options) {
        this.oauthOptions = options;
        return this;
    }

    /**
     * Sets OIDC for this builder.
     *
     * @param options the OIDC options
     * @return the builder instance
     */
    public HttpBuilder oidcOptions(Http.OIDC options) {
        this.oidcOptions = options;
        return this;
    }

    /**
     * Sets webhook verification for this builder.
     *
     * @param webhookVerification the webhook verification options
     * @return the builder instance
     */
    public HttpBuilder webhookVerification(Http.WebhookVerification webhookVerification) {
        this.webhookVerification = webhookVerification;
        return this;
    }

    public Http.Scheme getScheme() {
        return scheme;
    }

    public Optional<String> getSchemeName() {
        return Optional.ofNullable(scheme).map((s) -> s.name);
    }

    /**
     * Returns the domain on this builder.
     *
     * @return the domain
     */
    public Optional<String> getDomain() {
        return domain;
    }

    /**
     * Returns the mutual TLS certificate authority on this builder.
     *
     * @return the TLS certificate authority, in bytes.
     */
    public byte[] getMutualTLSCA() {
        return mutualTLSCA;
    }

    /**
     * Returns whether compression is enabled for this builder.
     *
     * @return {@code true} if compression is enabled, {@code false} otherwise
     */
    public boolean isCompression() {
        return compression;
    }

    /**
     * Returns whether WebSocket to TCP conversion is enabled for this builder.
     *
     * @return {@code true} if WebSocket to TCP conversion is enabled, {@code false} otherwise
     */
    public boolean isWebsocketTcpConversion() {
        return websocketTcpConversion;
    }

    /**
     * Returns the circuit breaker value for this builder.
     *
     * @return the circuit breaker value
     */
    public Optional<Double> getCircuitBreaker() {
        return circuitBreaker;
    }

    /**
     * Returns the list of request headers to add for this builder.
     *
     * @return the list of headers
     */
    public List<Http.Header> getRequestHeaders() {
        return requestHeaders;
    }

    /**
     * Returns the list of response headers to add for this builder.
     *
     * @return the list of headers
     */
    public List<Http.Header> getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * Returns the list of request headers to remove for this builder.
     *
     * @return the list of headers
     */
    public List<String> getRemoveRequestHeaders() {
        return removeRequestHeaders;
    }

    /**
     * Returns the list of response headers to remove for this builder.
     *
     * @return the list of headers
     */
    public List<String> getRemoveResponseHeaders() {
        return removeResponseHeaders;
    }

    /**
     * Returns the basic authentication options for this builder.
     *
     * @return the basic authentication options
     */
    public Http.BasicAuth getBasicAuth() {
        return basicAuthOptions;
    }

    /**
     * Returns the OAuth options for this builder.
     *
     * @return the OAuth options
     */
    public Http.OAuth getOauth() {
        return oauthOptions;
    }

    /**
     * Returns the OIDC options for this builder.
     *
     * @return the OIDC options
     */
    public Http.OIDC getOidc() {
        return oidcOptions;
    }

    /**
     * Returns the webhook verification options for this builder.
     *
     * @return the webhook verification options
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
