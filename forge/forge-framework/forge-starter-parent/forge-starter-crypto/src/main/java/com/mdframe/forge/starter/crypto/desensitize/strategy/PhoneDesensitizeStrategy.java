package com.mdframe.forge.starter.crypto.desensitize.strategy;

import cn.hutool.core.util.StrUtil;

/**
 * 手机号脱敏策略
 * 保留前3位和后4位，中间用*代替
 * 例如：138****1234
 *
 * @author forge
 */
public class PhoneDesensitizeStrategy implements DesensitizeStrategy {

    @Override
    public String desensitize(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        if (value.length() < 7) {
            return value;
        }
        return StrUtil.hide(value, 3, value.length() - 4);
    }
}
