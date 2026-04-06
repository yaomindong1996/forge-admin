package com.mdframe.forge.starter.idempotent.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 幂等策略枚举
 * 
 * @description 定义不同的幂等处理策略
 * @author Forge Framework
 */
@Getter
@AllArgsConstructor
public enum IdempotentStrategy {

    /**
     * 严格拒绝重复请求
     * 对于同一幂等键的重复请求直接抛出异常，不返回任何结果
     */
    STRICT("strict", "严格拒绝重复请求"),

    /**
     * 返回上次缓存结果
     * 对于同一幂等键的重复请求，返回上次执行的缓存结果
     * 需要配合cacheResult=true使用
     */
    RETURN_CACHE("return_cache", "返回上次缓存结果"),

    /**
     * 必须携带有效Token
     * 请求必须携带有效的幂等Token才能执行
     * Token由服务端生成并下发给客户端
     */
    TOKEN_REQUIRED("token_required", "必须携带有效Token");

    /**
     * 策略代码
     */
    private final String code;

    /**
     * 策略描述
     */
    private final String description;

    /**
     * 根据代码获取策略枚举
     *
     * @param code 策略代码
     * @return 对应的策略枚举，如果不存在则返回 RETURN_CACHE
     */
    public static IdempotentStrategy fromCode(String code) {
        if (code == null) {
            return RETURN_CACHE;
        }
        for (IdempotentStrategy strategy : values()) {
            if (strategy.getCode().equals(code)) {
                return strategy;
            }
        }
        return RETURN_CACHE;
    }

    /**
     * 验证策略代码是否有效
     *
     * @param code 策略代码
     * @return 是否有效
     */
    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        for (IdempotentStrategy strategy : values()) {
            if (strategy.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
