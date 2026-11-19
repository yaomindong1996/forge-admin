package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

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
    @NotBlank(message = "租户名称不能为空")
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
     * 默认业务数据源ID
     */
    private Long defaultBusinessDatasourceId;

    /**
     * 默认业务数据源编码
     */
    private String defaultBusinessDatasourceCode;

    /**
     * 主题配置
     */
    private String themeConfig;

    /**
     * 开通套餐 ID（仅新增有效；为空则用默认套餐）
     */
    private Long packageId;

    /**
     * 新建时勾选的基础数据包（仅新增有效）。
     * <ul>
     *   <li>{@code null}：使用套餐解析结果（无套餐则推荐默认集）</li>
     *   <li>空列表：不引导任何基础数据</li>
     *   <li>非空：仅引导勾选项（可在套餐默认值上微调）</li>
     * </ul>
     */
    private List<String> bootstrapPackages;

}
