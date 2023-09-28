package com.ngrok;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class EndpointBuilder<T extends EndpointBuilder> extends MetadataBuilder<T> {
    private final List<String> allowCIDR = new ArrayList<>();
    private final List<String> denyCIDR = new ArrayList<>();
    private ProxyProto proxyProto = ProxyProto.None;
    private Optional<String> forwardsTo = Optional.empty();

    // Chainable methods

    public T allowCIDR(String allowCIDR) {
        this.allowCIDR.add(allowCIDR);
        return (T) this;
    }

    public T denyCIDR(String denyCIDR) {
        this.denyCIDR.add(denyCIDR);
        return (T) this;
    }

    public T proxyProto(ProxyProto proxyProto) {
        this.proxyProto = Objects.requireNonNull(proxyProto);
        return (T) this;
    }

    public T forwardsTo(String forwardsTo) {
        this.forwardsTo = Optional.of(forwardsTo);
        return (T) this;
    }

    // Accessors

    public List<String> getAllowCIDR() {
        return allowCIDR;
    }

    public List<String> getDenyCIDR() {
        return denyCIDR;
    }

    public ProxyProto getProxyProto() {
        return proxyProto;
    }

    public long getProxyProtoVersion() {
        return proxyProto.version();
    }

    public Optional<String> getForwardsTo() {
        return forwardsTo;
    }
}
