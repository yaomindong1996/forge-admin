package com.mdframe.forge.report.project.template.domain;

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
import java.util.Date;

/**
 * Go-View 模板实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report_template")
public class ReportTemplate extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    @TableId(value = "id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 来源项目ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long sourceProjectId;

    /**
     * 所属目录ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long directoryId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 封面图
     */
    private String indexImg;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 画布宽度
     */
    private Integer canvasWidth;

    /**
     * 画布高度
     */
    private Integer canvasHeight;

    /**
     * 背景颜色
     */
    private String backgroundColor;

    /**
     * 模板组件数据
     */
    private String componentData;

    /**
     * 发布状态（0未发布 1已发布）
     */
    private String publishStatus;

    /**
     * 模板范围（0私有 1公开）
     */
    private String templateScope;

    /**
     * 发布地址
     */
    private String publishUrl;

    /**
     * 发布时间
     */
    private Date publishTime;

    /**
     * 删除标志（0正常 1删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 被复制次数
     */
    private Integer copiedCount;

    /**
     * 拥有者ID
     */
    @TableField(exist = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long ownerId;

    /**
     * 拥有者名称
     */
    @TableField(exist = false)
    private String ownerName;
}
