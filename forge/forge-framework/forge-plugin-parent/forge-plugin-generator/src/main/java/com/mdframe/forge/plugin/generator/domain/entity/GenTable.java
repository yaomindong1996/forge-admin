package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 代码生成表配置实体类
 */
@Data
@TableName("gen_table")
public class GenTable implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 表ID
     */
    @TableId(value = "table_id", type = IdType.AUTO)
    private Long tableId;

    /**
     * 数据源ID
     */
    private Long datasourceId;

    /**
     * 表名称
     */
    private String tableName;

    /**
     * 表描述
     */
    private String tableComment;

    /**
     * 实体类名称
     */
    private String className;

    /**
     * 业务名称
     */
    private String businessName;

    /**
     * 功能名称
     */
    private String functionName;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 包路径
     */
    private String packageName;

    /**
     * 作者
     */
    private String author;

    /**
     * 生成方式
     */
    private String genType;

    /**
     * 生成路径
     */
    private String genPath;

    /**
     * 模板引擎
     */
    private String templateEngine;

    /**
     * 其他生成选项
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Map<String, Object> options;

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

    /**
     * 表字段列表
     */
    @TableField(exist = false)
    private List<GenTableColumn> columns;

    /**
     * 主键字段
     */
    @TableField(exist = false)
    private GenTableColumn pkColumn;

    /**
     * 树形结构父键字段
     */
    @TableField(exist = false)
    private String treeParentCode;

    /**
     * 树形结构编码字段
     */
    @TableField(exist = false)
    private String treeCode;

    /**
     * 树形结构名称字段
     */
    @TableField(exist = false)
    private String treeName;
}
