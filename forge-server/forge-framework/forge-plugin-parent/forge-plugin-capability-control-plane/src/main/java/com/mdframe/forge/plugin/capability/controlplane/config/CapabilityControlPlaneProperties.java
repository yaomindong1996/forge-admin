package com.mdframe.forge.plugin.capability.controlplane.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forge.capability")
public class CapabilityControlPlaneProperties {

    private String clientPepper;

    public String getClientPepper() {
        return clientPepper;
    }

    public void setClientPepper(String clientPepper) {
        this.clientPepper = clientPepper;
    }
}
