package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 代码生成模板配置实体类
 */
@Data
@TableName("gen_template")
public class GenTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 模板类型：ENTITY/MAPPER/SERVICE/CONTROLLER/DTO/VO/SQL
     */
    private String templateType;

    /**
     * 模板引擎：VELOCITY/FREEMARKER
     */
    private String templateEngine;

    /**
     * 模板内容
     */
    private String templateContent;

    /**
     * 生成文件后缀
     */
    private String fileSuffix;

    /**
     * 生成文件路径（相对路径）
     */
    private String filePath;

    /**
     * 是否系统内置（1-是，0-否）
     */
    private Integer isSystem;

    /**
     * 是否启用（1-启用，0-禁用）
     */
    private Integer isEnabled;

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
}
