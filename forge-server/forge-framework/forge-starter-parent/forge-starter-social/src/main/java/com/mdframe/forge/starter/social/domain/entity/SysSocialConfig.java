package com.mdframe.forge.starter.social.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mdframe.forge.starter.trans.annotation.DictTrans;
import com.mdframe.forge.starter.trans.annotation.TransField;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业协同连接配置表实体类（原三方登录配置，升级为连接根）
 */
@Data
@TableName("sys_social_config")
@DictTrans
public class SysSocialConfig {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 平台类型
     */
    @NotBlank(message = "平台类型不能为空")
    private String platform;

    /**
     * 平台名称
     */
    private String platformName;

    /**
     * 平台Logo
     */
    private String platformLogo;

    /**
     * 连接编码（租户内唯一）
     */
    private String connectionCode;

    /**
     * 连接名称
     */
    private String connectionName;

    /**
     * 外部企业ID（企业微信CorpId等）
     */
    private String enterpriseId;

    /**
     * 连接类型：CORP_INTERNAL自建应用/THIRD_PARTY第三方/OAUTH_ONLY仅登录
     */
    private String connectionType;

    /**
     * 身份匹配策略：BIND_ONLY仅绑定已有/AUTO_CREATE自动创建/MANUAL人工处理
     */
    private String identityPolicy;

    /**
     * 自动建号默认角色ID列表（逗号分隔），连接级覆盖全局配置
     */
    private String defaultRoleIds;

    /**
     * 目录权威来源：EXTERNAL外部权威/LOCAL本地权威/NONE不同步
     */
    private String directoryAuthority;

    /**
     * 目录同步默认挂载的根组织ID
     */
    private Long defaultOrgId;

    /**
     * API基础地址：为空使用平台官方地址，私有化部署可自定义
     */
    private String apiBaseUrl;

    /**
     * 待办卡片推送开关：1开启 0关闭
     */
    private Integer todoPushEnabled;

    /**
     * 待办H5访问地址：须在平台可信域名内，用于拼接待办详情深链
     */
    private String todoPushH5Url;

    /**
     * 工作台免登开关：1开启 0关闭。开启后客户端工作台可用该连接的 connectionCode 免登（替代前端写死 VITE_WECOM_CONNECTION_CODE）
     */
    private Integer ssoWorkbenchEnabled;

    /**
     * 定时目录同步开关：1开启 0关闭。开启后由连接管理自动维护对应定时任务
     */
    private Integer syncScheduleEnabled;

    /**
     * 定时目录同步 Cron 表达式：syncScheduleEnabled=1 时生效
     */
    private String syncCron;

    /**
     * 应用ID/Key（旧登录配置字段，兼容期保留，可空）。
     * 新连接不再在连接根填写该值，改由 sys_social_app_config.client_id 保存。
     */
    private String clientId;

    /**
     * 应用Secret（旧登录配置明文字段，兼容期可空只读）。
     * 新凭据统一存 sys_social_app_config 密文，新增连接无需填写本列。
     */
    private String clientSecret;

    /**
     * 回调地址
     */
    private String redirectUri;

    /**
     * 企业微信AgentId
     */
    private String agentId;

    /**
     * 授权范围
     */
    private String scope;

    /**
     * 状态（1-启用，0-停用）
     */
    @TransField(dictType = "enable_disable")
    private Integer status;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建组织ID
     */
    private Long createDept;

    /**
     * 逻辑删除标记：0正常，删除后写当前行主键
     */
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;

    // ========== 字段名称映射 ==========

    @TableField(exist = false)
    private String statusName;
}
