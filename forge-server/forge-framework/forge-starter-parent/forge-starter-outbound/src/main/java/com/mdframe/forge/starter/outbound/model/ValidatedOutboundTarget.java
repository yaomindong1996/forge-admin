package com.mdframe.forge.starter.outbound.model;

import com.mdframe.forge.starter.outbound.security.IpAddressClassifier;
import lombok.Getter;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;

@Getter
public class ValidatedOutboundTarget {

    private final String scene;
    private final URI uri;
    private final String host;
    private final int port;
    private final List<InetAddress> addresses;
    private final IpAddressClassifier.AddressType addressType;

    public ValidatedOutboundTarget(String scene, URI uri, String host, int port,
                                   List<InetAddress> addresses,
                                   IpAddressClassifier.AddressType addressType) {
        this.scene = scene;
        this.uri = uri;
        this.host = host;
        this.port = port;
        this.addresses = List.copyOf(addresses);
        this.addressType = addressType;
    }
}
