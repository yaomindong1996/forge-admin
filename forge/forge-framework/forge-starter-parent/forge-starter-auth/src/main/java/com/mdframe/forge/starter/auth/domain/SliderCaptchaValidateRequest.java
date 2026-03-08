package com.mdframe.forge.starter.auth.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 滑块验证码验证请求
 */
@Data
public class SliderCaptchaValidateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码key
     */
    private String codeKey;

    /**
     * 滑块移动距离（x轴偏移量）
     */
    private Integer moveX;

    /**
     * 滑动轨迹（用于行为分析，可选）
     */
    private String track;
}
