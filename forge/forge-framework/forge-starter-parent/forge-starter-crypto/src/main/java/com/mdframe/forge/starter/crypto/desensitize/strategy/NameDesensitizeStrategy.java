package com.mdframe.forge.starter.crypto.desensitize.strategy;

import cn.hutool.core.util.StrUtil;

/**
 * 姓名脱敏策略
 * 保留首字符，后面用*代替
 * 例如：张**
 *
 * @author forge
 */
public class NameDesensitizeStrategy implements DesensitizeStrategy {

    @Override
    public String desensitize(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        if (value.length() == 1) {
            return value;
        }
        return StrUtil.hide(value, 1, value.length());
    }
}
