package com.mdframe.forge.starter.crypto.desensitize.strategy;

import cn.hutool.core.util.StrUtil;

/**
 * 车牌号脱敏策略
 * 保留前2位和后2位，中间用*代替
 * 例如：京A****12
 *
 * @author forge
 */
public class CarLicenseDesensitizeStrategy implements DesensitizeStrategy {

    @Override
    public String desensitize(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        if (value.length() < 4) {
            return value;
        }
        return StrUtil.hide(value, 2, value.length() - 2);
    }
}
