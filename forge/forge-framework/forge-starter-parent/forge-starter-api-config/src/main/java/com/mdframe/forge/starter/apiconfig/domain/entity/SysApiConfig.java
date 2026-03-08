package com.mdframe.forge.starter.apiconfig.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mdframe.forge.starter.trans.annotation.DictTrans;
import com.mdframe.forge.starter.trans.annotation.TransField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * REST接口配置管理表实体类
 */
@Data
@TableName("sys_api_config")
@DictTrans
public class SysApiConfig {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 接口名称 (如:查询用户信息)
     */
    private String apiName;

    /**
     * 接口编码 (用于程序逻辑引用)
     */
    private String apiCode;

    /**
     * 请求方式 (GET, POST, PUT, DELETE, ALL)
     */
    private String reqMethod;

    /**
     * 接口请求路径 (支持Ant风格, 如 /api/user/**)
     */
    private String urlPath;

    /**
     * 接口版本号
     */
    private String apiVersion;

    /**
     * 所属业务模块 (如: sys, order, pay)
     */
    private String moduleCode;

    /**
     * 所属微服务ID (若为微服务架构)
     */
    private String serviceId;

    /**
     * 是否需认证/鉴权 (1-需要, 0-不需要)
     */
    @TransField(dictType = "yes_no")
    private Integer authFlag;

    /**
     * 是否需报文加解密 (1-需要, 0-不需要)
     */
    @TransField(dictType = "yes_no")
    private Integer encryptFlag;

    /**
     * 是否启用租户隔离 (1-启用, 0-不启用)
     */
    @TransField(dictType = "yes_no")
    private Integer tenantFlag;

    /**
     * 是否开启限流 (1-开启, 0-关闭)
     */
    @TransField(dictType = "yes_no")
    private Integer limitFlag;

    /**
     * 需脱敏字段 (JSON数组存储, 如 ["phone", "id_card"])
     */
    private String sensitiveFields;

    /**
     * 状态 (1-正常, 0-停用)
     */
    @TransField(dictType = "enable_disable")
    private Integer status;

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

    // ========== 字段名称映射 ==========

    @TableField(exist = false)
    private String authFlagName;

    @TableField(exist = false)
    private String encryptFlagName;

    @TableField(exist = false)
    private String tenantFlagName;

    @TableField(exist = false)
    private String limitFlagName;

    @TableField(exist = false)
    private String statusName;
}
