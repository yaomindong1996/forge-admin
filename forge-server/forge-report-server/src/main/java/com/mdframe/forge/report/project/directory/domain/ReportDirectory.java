package com.mdframe.forge.report.project.directory.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 报表目录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report_directory")
public class ReportDirectory extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 父级目录ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parentId;

    /**
     * 祖级链，逗号分隔
     */
    private String ancestors;

    /**
     * 目录名称
     */
    private String directoryName;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志（0正常 1删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 子目录
     */
    @TableField(exist = false)
    private List<ReportDirectory> children;
}
