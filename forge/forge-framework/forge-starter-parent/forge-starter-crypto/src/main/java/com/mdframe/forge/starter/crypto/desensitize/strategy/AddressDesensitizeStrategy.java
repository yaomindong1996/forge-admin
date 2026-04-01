package com.mdframe.forge.starter.crypto.desensitize.strategy;

import cn.hutool.core.util.StrUtil;

/**
 * 地址脱敏策略
 * 保留前6位，后面用*代替
 * 例如：北京市海淀********
 *
 * @author forge
 */
public class AddressDesensitizeStrategy implements DesensitizeStrategy {

    @Override
    public String desensitize(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        if (value.length() <= 6) {
            return value;
        }
        return StrUtil.hide(value, 6, value.length());
    }
}
