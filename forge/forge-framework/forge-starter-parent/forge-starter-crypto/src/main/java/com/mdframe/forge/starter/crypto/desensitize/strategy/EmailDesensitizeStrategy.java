package com.mdframe.forge.starter.crypto.desensitize.strategy;

import cn.hutool.core.util.StrUtil;

/**
 * 邮箱脱敏策略
 * 保留@前的首字符，后面用*代替
 * 例如：u***@example.com
 *
 * @author forge
 */
public class EmailDesensitizeStrategy implements DesensitizeStrategy {

    @Override
    public String desensitize(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        int index = value.indexOf('@');
        if (index <= 1) {
            return value;
        }
        return StrUtil.hide(value, 1, index) + value.substring(index);
    }
}
