package com.mdframe.forge.starter.idempotent.constant;

/**
 * 幂等策略枚举
 * 
 * @description 定义不同的幂等处理策略
 * @author Forge Framework
 */
public enum IdempotentStrategy {

    /**
     * 严格拒绝重复请求
     * 对于同一幂等键的重复请求直接抛出异常，不返回任何结果
     */
    STRICT("严格拒绝重复请求"),

    /**
     * 返回上次缓存结果
     * 对于同一幂等键的重复请求，返回上次执行的缓存结果
     * 需要配合cacheResult=true使用
     */
    RETURN_CACHE("返回上次缓存结果"),

    /**
     * 必须携带有效Token
     * 请求必须携带有效的幂等Token才能执行
     * Token由服务端生成并下发给客户端
     */
    TOKEN_REQUIRED("必须携带有效Token");

    private final String description;

    IdempotentStrategy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
