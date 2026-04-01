package com.mdframe.forge.starter.crypto.desensitize.strategy;

/**
 * 脱敏策略接口
 *
 * @author forge
 */
public interface DesensitizeStrategy {

    /**
     * 脱敏处理
     *
     * @param value 原始值
     * @return 脱敏后的值
     */
    String desensitize(String value);
}
