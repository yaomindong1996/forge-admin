package com.mdframe.forge.plugin.capability.identity.oauth;

@FunctionalInterface
public interface ExactRedirectUriRegistry {

    boolean contains(Long tenantId, Long clientId, String redirectUri);
}
