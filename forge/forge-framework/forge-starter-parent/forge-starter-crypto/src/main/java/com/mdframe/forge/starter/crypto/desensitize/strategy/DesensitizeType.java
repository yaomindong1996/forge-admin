package com.mdframe.forge.starter.crypto.desensitize.strategy;

/**
 * 脱敏类型枚举
 *
 * @author forge
 */
public enum DesensitizeType {

    /**
     * 自定义脱敏
     */
    CUSTOM,

    /**
     * 手机号脱敏
     */
    PHONE,

    /**
     * 身份证号脱敏
     */
    ID_CARD,

    /**
     * 邮箱脱敏
     */
    EMAIL,

    /**
     * 银行卡号脱敏
     */
    BANK_CARD,

    /**
     * 姓名脱敏
     */
    NAME,

    /**
     * 地址脱敏
     */
    ADDRESS,

    /**
     * 密码脱敏
     */
    PASSWORD,

    /**
     * 车牌号脱敏
     */
    CAR_LICENSE
}
