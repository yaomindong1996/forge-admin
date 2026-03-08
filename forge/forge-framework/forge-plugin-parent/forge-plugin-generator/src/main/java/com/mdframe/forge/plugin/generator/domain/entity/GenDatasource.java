package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 代码生成器数据源配置实体类
 */
@Data
@TableName("gen_datasource")
public class GenDatasource implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据源ID
     */
    @TableId(value = "datasource_id", type = IdType.AUTO)
    private Long datasourceId;

    /**
     * 数据源名称
     */
    private String datasourceName;

    /**
     * 数据源编码（唯一标识）
     */
    private String datasourceCode;

    /**
     * 数据库类型：MySQL/Oracle/PostgreSQL/SQLServer
     */
    private String dbType;

    /**
     * 驱动类名
     */
    private String driverClassName;

    /**
     * JDBC连接地址
     */
    private String url;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（加密存储）
     */
    private String password;

    /**
     * 是否默认数据源（1-是，0-否）
     */
    private Integer isDefault;

    /**
     * 是否启用（1-启用，0-禁用）
     */
    private Integer isEnabled;

    /**
     * 测试查询SQL
     */
    private String testQuery;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 更新者
     */
    private String updateBy;
}
