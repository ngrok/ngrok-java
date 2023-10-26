package com.ngrok;

import java.util.*;

/**
 * A set of data classes to support creation of HTTP endpoint listeners.
 */
public interface Http {
    /**
     * Represents the scheme for an HTTP listener.
     */
    enum Scheme {
        HTTP("HTTP"),
        HTTPS("HTTPS");

        /**
         * The name of the scheme.
         */
        public final String name;

        Scheme(String name) {
            this.name = name;
        }
    }

    /**
     * Represents an HTTP header.
     */
    class Header {
        private final String name;
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
     * Represents basic authentication options for an HTTP listener.
     *
     * @see <a href="https://ngrok.com/docs/http/basic-auth/">Basic Auth</a>
     * in the ngrok docs for additional details.
     */
    class BasicAuth {
        private final String username;
        private final String password;

        /**
         * Constructs a new set of basic authentication options with the specified
         * username and password.
         *
         * @param username the username
         * @param password the password
         */
        public BasicAuth(String username, String password) {
            this.username = Objects.requireNonNull(username);
            this.password = Objects.requireNonNull(password);
        }

        /**
         * Returns the username for basic auth.
         *
         * @return the username
         */
        public String getUsername() {
            return username;
        }

        /**
         * Returns the password for basic auth.
         *
         * @return the password
         */
        public String getPassword() {
            return password;
        }
    }

    /**
     * Represents OAuth configuration for an HTTP listener.
     *
     * @see <a href="https://ngrok.com/docs/http/oauth/">OAuth</a>
     * in the ngrok docs for additional details.
     */
    class OAuth {
        private final String provider;
        private String clientId;
        private String clientSecret;
        private final List<String> allowEmails = new ArrayList<>();
        private final List<String> allowDomains = new ArrayList<>();
        private final List<String> scopes = new ArrayList<>();

        /**
         * Constructs new OAuth configuration with the specified provider.
         *
         * @param provider the provider for OAuth
         */
        public OAuth(String provider) {
            this.provider = Objects.requireNonNull(provider);
        }

        /**
         * Sets the client ID and client secret for OAuth.
         *
         * @param id     the client ID for the OAuth
         * @param secret the client secret for the OAuth
         * @return this OAuth object
         */
        public OAuth client(String id, String secret) {
            this.clientId = Objects.requireNonNull(id);
            this.clientSecret = Objects.requireNonNull(secret);
            return this;
        }

        /**
         * Sets the email address allowed by OAuth.
         *
         * @param email the email address allowed by OAuth
         * @return this OAuth object
         */
        public OAuth allowEmail(String email) {
            allowEmails.add(Objects.requireNonNull(email));
            return this;
        }

        /**
         * Sets the domain allowed by the OAuth.
         *
         * @param domain the domain allowed by OAuth
         * @return this OAuth object
         */
        public OAuth allowDomain(String domain) {
            allowDomains.add(Objects.requireNonNull(domain));
            return this;
        }

        /**
         * Sets the scope for OAuth.
         *
         * @param scope the scope for OAuth
         * @return this OAuth object
         */
        public OAuth scope(String scope) {
            scopes.add(Objects.requireNonNull(scope));
            return this;
        }

        /**
         * Returns the OAuth provider.
         *
         * @return the provider
         */
        public String getProvider() {
            return provider;
        }

        /**
         * Returns of client ID and secret have been configured for OAuth
         *
         * @return true if both client ID and secret has been set, false otherwise
         */
        public boolean hasClientConfigured() {
            return clientId != null && clientSecret != null;
        }

        /**
         * Returns the client ID for OAuth.
         *
         * @return the client ID
         */
        public String getClientId() {
            return clientId;
        }

        /**
         * Returns the client secret for OAuth.
         *
         * @return the client secret
         */
        public String getClientSecret() {
            return clientSecret;
        }

        /**
         * Returns the email address to be allowed by OAuth.
         *
         * @return the email address
         */
        public List<String> getAllowEmails() {
            return allowEmails;
        }

        /**
         * Returns the domain to be allowed by OAuth.
         *
         * @return the domain
         */
        public List<String> getAllowDomains() {
            return allowDomains;
        }

        /**
         * Returns the scope to be used by OAuth.
         *
         * @return the scope
         */
        public List<String> getScopes() {
            return scopes;
        }
    }

    /**
     * Represents OIDC configuration for an HTTP listener.
     *
     * @see <a href="https://ngrok.com/docs/http/openid-connect/">OpenID Connect</a>
     * in the ngrok docs for additional details.
     */
    class OIDC {
        private final String issuerUrl;
        private final String clientId;
        private final String clientSecret;
        private final List<String> allowEmails = new ArrayList<>();
        private final List<String> allowDomains = new ArrayList<>();
        private final List<String> scopes = new ArrayList<>();

        /**
         * Constructs a new OIDC configuration with the specified
         * issuer URL, client ID, and client secret.
         *
         * @param issuerUrl    the issuer URL
         * @param clientId     the client ID
         * @param clientSecret the client secret
         */
        public OIDC(String issuerUrl, String clientId, String clientSecret) {
            this.issuerUrl = Objects.requireNonNull(issuerUrl);
            this.clientId = Objects.requireNonNull(clientId);
            this.clientSecret = Objects.requireNonNull(clientSecret);
        }

        /**
         * Sets the email address that will be allowed by OIDC.
         *
         * @param email the email address, unused if {@code null}
         * @return this OIDC object
         */
        public OIDC allowEmail(String email) {
            allowEmails.add(Objects.requireNonNull(email));
            return this;
        }

        /**
         * Sets the domain that will be allowed by OIDC.
         *
         * @param domain the domain, unused if {@code null}
         * @return this OIDC object
         */
        public OIDC allowDomain(String domain) {
            allowDomains.add(Objects.requireNonNull(domain));
            return this;
        }

        /**
         * Sets the scope to be used by OIDC.
         *
         * @param scope the scope, unused if {@code null}
         * @return this OIDC object
         */
        public OIDC scope(String scope) {
            scopes.add(Objects.requireNonNull(scope));
            return this;
        }

        /**
         * Returns the issuer URL for OIDC.
         *
         * @return the issuer URL
         */
        public String getIssuerUrl() {
            return issuerUrl;
        }

        /**
         * Returns the client ID for OIDC.
         *
         * @return the client ID
         */
        public String getClientId() {
            return clientId;
        }

        /**
         * Returns the client secret for OIDC.
         *
         * @return the client secret
         */
        public String getClientSecret() {
            return clientSecret;
        }

        /**
         * Returns the email address to be allowed by OIDC.
         *
         * @return the email address
         */
        public List<String> getAllowEmail() {
            return allowEmails;
        }

        /**
         * Returns the domain to be allowed by OIDC.
         *
         * @return the domain
         */
        public List<String> getAllowDomain() {
            return allowDomains;
        }

        /**
         * Returns the scope to be used by OIDC.
         *
         * @return the scope
         */
        public List<String> getScope() {
            return scopes;
        }
    }

    /**
     * Represents webhook verification options for an HTTP listener.
     *
     * @see <a href="https://ngrok.com/docs/http/webhook-verification/">Webhook Verification</a>
     * in the ngrok docs for additional details.
     */
    class WebhookVerification {
        private final String provider;
        private final String secret;

        /**
         * Constructs a new set of webhook verification options with the specified
         * provider and secret.
         *
         * @param provider the provider
         * @param secret   the secret
         */
        public WebhookVerification(String provider, String secret) {
            this.provider = Objects.requireNonNull(provider);
            this.secret = Objects.requireNonNull(secret);
        }

        /**
         * Returns the provider for the webhook verification.
         *
         * @return the provider
         */
        public String getProvider() {
            return provider;
        }

        /**
         * Returns the secret for the webhook verification.
         *
         * @return the secret
         */
        public String getSecret() {
            return secret;
        }
    }
}
