package com.mdframe.forge.starter.cache.managed.context;

@FunctionalInterface
public interface CacheIdentityProvider {

    CacheIdentity current();
}
