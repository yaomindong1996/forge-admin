package com.mdframe.forge.starter.crypto.desensitize.strategy;

import cn.hutool.core.util.StrUtil;

/**
 * 密码脱敏策略
 * 全部替换为*
 * 例如：******
 *
 * @author forge
 */
public class PasswordDesensitizeStrategy implements DesensitizeStrategy {

    @Override
    public String desensitize(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        return StrUtil.repeat('*', value.length());
    }
}
