package com.ngrok;

import java.util.Objects;

public interface Http {

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
     * A class representing an HTTP header.
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

    class BasicAuth {

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
        public BasicAuth(String username, String password) {
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
    class OAuth {

        private final String provider;

        private String clientId;

        private String clientSecret;

        private String allowEmail;

        private String allowDomain;

        private String scope;

        /**
         * Constructs new OAuth options with the specified provider.
         *
         * @param provider the provider of the OAuth options
         */
        public OAuth(String provider) {
            this.provider = Objects.requireNonNull(provider);
        }

        /**
         * Sets the client ID and client secret for the OAuth options.
         *
         * @param id     the client ID for the OAuth options
         * @param secret the client secret for the OAuth options
         * @return this OAuthOptions object
         */
        public OAuth client(String id, String secret) {
            this.clientId = Objects.requireNonNull(id);
            this.clientSecret = Objects.requireNonNull(secret);
            return this;
        }

        /**
         * Sets the email address allowed by OAuth.
         *
         * @param email the email address allowed by the OAuth options
         * @return this OAuthOptions object
         */
        public OAuth allowEmail(String email) {
            this.allowEmail = Objects.requireNonNull(email);
            return this;
        }

        /**
         * Sets the domain allowed by the OAuth options.
         *
         * @param domain the domain allowed by the OAuth options
         * @return this OAuthOptions object
         */
        public OAuth allowDomain(String domain) {
            this.allowDomain = Objects.requireNonNull(domain);
            return this;
        }

        /**
         * Sets the scope of the OAuth options.
         *
         * @param scope the scope of the OAuth options
         * @return this OAuthOptions object
         */
        public OAuth scope(String scope) {
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
         * otherwise
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
    class OIDC {
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
        public OIDC(String issuerUrl, String clientId, String clientSecret) {
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
        public OIDC allowEmail(String email) {
            this.allowEmail = Objects.requireNonNull(email);
            return this;
        }

        /**
         * Sets the domain allowed by the OIDC options.
         *
         * @param domain the domain allowed by the OIDC options
         * @return this OIDCOptions object
         */
        public OIDC allowDomain(String domain) {
            this.allowDomain = Objects.requireNonNull(domain);
            return this;
        }

        /**
         * Sets the scope of the OIDC options.
         *
         * @param scope the scope of the OIDC options
         * @return this OIDCOptions object
         */
        public OIDC scope(String scope) {
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
         * otherwise
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
    class WebhookVerification {
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
}
