package com.mdframe.forge.plugin.capability.identity.oauth;

public interface DelegationAuthorizationCodeStore {

    String issue(DelegationAuthorizationCode authorizationCode);

    DelegationAuthorizationCode consume(String rawCode);
}
