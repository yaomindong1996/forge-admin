package com.mdframe.forge.plugin.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户新增/修改DTO
 */
@Data
public class SysTenantDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID（修改时必传）
     */
    private Long id;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 负责人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 租户人员数量上限（0表示无限制）
     */
    private Integer userLimit;

    /**
     * 租户状态（0-禁用，1-正常）
     */
    private Integer tenantStatus;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 租户描述
     */
    private String tenantDesc;

    /**
     * 浏览器icon（存储图标URL/Base64）
     */
    private String browserIcon;

    /**
     * 浏览器标签名称
     */
    private String browserTitle;

    /**
     * 系统名称
     */
    private String systemName;

    /**
     * 系统logo（存储logo URL/Base64）
     */
    private String systemLogo;

    /**
     * 系统介绍
     */
    private String systemIntro;

    /**
     * 版权显示文本
     */
    private String copyrightInfo;

    /**
     * 系统布局（default-默认，classic-经典，modern-现代等）
     */
    private String systemLayout;

    /**
     * 系统主题（light-亮色，dark-暗色，auto-跟随系统等）
     */
    private String systemTheme;

    /**
     * 主题配置
     */
    private String themeConfig;


}
