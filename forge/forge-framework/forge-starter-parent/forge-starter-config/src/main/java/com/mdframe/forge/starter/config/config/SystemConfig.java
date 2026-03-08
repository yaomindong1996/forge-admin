package com.mdframe.forge.starter.config.config;

import lombok.Data;

/**
 * 系统配置
 */
@Data
public class SystemConfig {
    /**
     * 系统名称
     */
    private String systemName = "Forge Admin";

    /**
     * 系统版本
     */
    private String systemVersion = "1.0.0";

    /**
     * 版权信息
     */
    private String copyright = "© 2023-2024 Forge Admin. All rights reserved.";

    /**
     * 是否启用水印
     */
    private Boolean enableWatermark = true;

    /**
     * 水印内容
     */
    private String watermarkContent = "系统水印";

    /**
     * 水印透明度
     */
    private Double watermarkOpacity = 0.5;
}