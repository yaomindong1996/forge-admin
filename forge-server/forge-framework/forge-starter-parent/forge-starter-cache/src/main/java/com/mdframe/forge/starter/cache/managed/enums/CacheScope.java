package com.mdframe.forge.starter.cache.managed.enums;

/**
 * 缓存键的可信身份隔离范围。
 */
public enum CacheScope {
    GLOBAL,
    TENANT,
    TENANT_USER,
    TENANT_USER_ORG
}
