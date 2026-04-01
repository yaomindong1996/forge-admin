package com.mdframe.forge.starter.crypto.desensitize.strategy;

import cn.hutool.core.util.StrUtil;

/**
 * 银行卡号脱敏策略
 * 保留前6位和后4位，中间用*代替
 * 例如：622202********1234
 *
 * @author forge
 */
public class BankCardDesensitizeStrategy implements DesensitizeStrategy {

    @Override
    public String desensitize(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        if (value.length() < 10) {
            return value;
        }
        return StrUtil.hide(value, 6, value.length() - 4);
    }
}
