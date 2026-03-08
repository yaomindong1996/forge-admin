package com.mdframe.forge.starter.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 滑块验证码响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SliderCaptchaResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码key
     */
    private String codeKey;

    /**
     * 背景图片（Base64编码）
     */
    private String backgroundImage;

    /**
     * 滑块图片（Base64编码）
     */
    private String sliderImage;

    /**
     * 滑块图片宽度
     */
    private Integer sliderWidth;

    /**
     * 滑块图片高度
     */
    private Integer sliderHeight;

    /**
     * 背景图片宽度
     */
    private Integer backgroundWidth;

    /**
     * 背景图片高度
     */
    private Integer backgroundHeight;

    /**
     * 缺口Y坐标位置（用于前端显示滑块）
     */
    private Integer notchY;

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 验证码类型
     */
    private String captchaType = "slider";
}
