package cn.dev33.satoken.dao;

import cn.dev33.satoken.session.SaSession;

/**
 * Sa-Token ≤1.38 {@code sa-token-redis-jackson} 写入 Redis 的 Session 类型。
 * <p>
 * 1.45.0 起该类从官方 jar 移除，但存量 Redis 仍带
 * {@code "@class":"cn.dev33.satoken.dao.SaSessionForJacksonCustomized"}。
 * Boot4 + Jackson3 反序列化时按类名解析，缺类会直接导致登录态读失败。
 * 保留空子类仅为兼容读旧数据，新写入走 {@link SaSession}。
 */
public class SaSessionForJacksonCustomized extends SaSession {

    private static final long serialVersionUID = -7852311037327718900L;

    public SaSessionForJacksonCustomized() {
    }

    public SaSessionForJacksonCustomized(String id) {
        super(id);
    }
}
