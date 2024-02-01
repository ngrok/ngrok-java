package com.ngrok;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An abstract builder sharing common attributes of endpoint listener builders.
 *
 * @param <T> the concrete builder impl to return to satisfy the builder pattern
 */
public abstract class EndpointBuilder<T extends EndpointBuilder<T>> extends MetadataBuilder<T> {
    private final List<String> allowCIDR = new ArrayList<>();
    private final List<String> denyCIDR = new ArrayList<>();
    private ProxyProto proxyProto = ProxyProto.None;
    private Optional<String> policy = Optional.empty();

    /**
     * Adds a CIDR to the list of allowed CIDRs for this endpoint.
     *
     * @param allowCIDR The parameter "allowCIDR" is a string that represents a
     *                  Classless Inter-Domain Routing (CIDR) notation. It is used to specify
     *                  a range of IP addresses that are allowed. For example, 10.0.0.0/24
     * @return An instance the builder represented by type T
     *
     * @see <a href="https://ngrok.com/docs/http/ip-restrictions/">IP Restrictions</a>
     * in the ngrok docs for additional details.
     */
    public T allowCIDR(String allowCIDR) {
        this.allowCIDR.add(allowCIDR);
        return (T) this;
    }

    /**
     * Adds a CIDR to the list of denied CIDRs for this endpoint.
     *
     * @param denyCIDR The parameter "denyCIDR" is a string that represents a
     *                 Classless Inter-Domain Routing (CIDR) notation. It is used to specify a
     *                 range of IP addresses that should be denied access. For example, 10.0.0.0/24
     * @return An instance the builder represented by type T
     *
     * @see <a href="https://ngrok.com/docs/http/ip-restrictions/">IP Restrictions</a>
     * in the ngrok docs for additional details.
     */
    public T denyCIDR(String denyCIDR) {
        this.denyCIDR.add(denyCIDR);
        return (T) this;
    }

    /**
     * Sets the proxy protocol for this endpoint.
     *
     * @param proxyProto the proxy protocol for the builder
     * @return An instance the builder represented by type T
     */
    public T proxyProto(ProxyProto proxyProto) {
        this.proxyProto = Objects.requireNonNull(proxyProto);
        return (T) this;
    }

    /**
     * Sets the policy for this endpoint.
     *
     * @param policy the policy for the builder
     * @return An instance the builder represented by type T
     */
    public T policy(final String policy) {
        this.policy = Optional.ofNullable(policy);
        return (T) this;
    }

    /**
     * Returns a list of strings representing allowed CIDR addresses for this endpoint.
     *
     * @return the currently set allow CIDR addresses
     */
    public List<String> getAllowCIDR() {
        return allowCIDR;
    }

    /**
     * Returns a list of strings representing denied CIDR addresses for this endpoint.
     *
     * @return the currently set deny CIDR addresses
     */
    public List<String> getDenyCIDR() {
        return denyCIDR;
    }

    /**
     * Returns the proxy protocol for this builder.
     *
     * @return the currently set proxy protocol
     */
    public ProxyProto getProxyProto() {
        return proxyProto;
    }

    /**
     * Returns the version of the proxy protocol for this endpoint.
     *
     * @return the currently set version of the proxy protocol
     */
    public long getProxyProtoVersion() {
        return proxyProto.version();
    }

    /**
     * Returns the policy for this endpoint.
     *
     * @return the currently set policy
     */
    public Optional<String> getPolicy() {
        return this.policy;
    }
}
