package com.mdframe.forge.starter.config.config;

import lombok.Data;

/**
 * 水印配置
 */
@Data
public class WatermarkConfig {

    /**
     * 是否启用水印
     */
    private Boolean enable = false;

    /**
     * 水印内容
     */
    private String content = "MDFrame";

    /**
     * 水印透明度 (0.0-1.0)
     */
    private Double opacity = 0.1;

    /**
     * 水印字体大小
     */
    private Integer fontSize = 16;

    /**
     * 水印字体颜色
     */
    private String fontColor = "#cccccc";

    /**
     * 水印旋转角度
     */
    private Integer rotate = -20;

    /**
     * 水印间距X轴
     */
    private Integer gapX = 200;

    /**
     * 水印间距Y轴
     */
    private Integer gapY = 200;

    /**
     * 水印偏移X轴
     */
    private Integer offsetX = 0;

    /**
     * 水印偏移Y轴
     */
    private Integer offsetY = 0;

    /**
     * 水印层级
     */
    private Integer zIndex = 1000;

    /**
     * 是否显示时间戳
     */
    private Boolean showTimestamp = false;

    /**
     * 时间戳格式
     */
    private String timestampFormat = "yyyy-MM-dd HH:mm:ss";
}