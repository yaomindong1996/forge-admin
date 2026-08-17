package com.mdframe.forge.starter.cache.managed.context;

import com.mdframe.forge.starter.core.session.SessionHelper;

/**
 * 从可信执行身份或登录态读取缓存隔离维度。
 */
public class SessionCacheIdentityProvider implements CacheIdentityProvider {

    @Override
    public CacheIdentity current() {
        try {
            return new CacheIdentity(
                    SessionHelper.getTenantId(),
                    SessionHelper.getUserId(),
                    SessionHelper.getActiveOrgId());
        } catch (RuntimeException exception) {
            return new CacheIdentity(null, null, null);
        }
    }
}
