package com.mdframe.forge.report.project.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Go-View 项目实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report_project")
public class GoviewProject extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 封面图URL
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
     * 组件列表JSON
     */
    private String componentData;

    /**
     * 发布状态（0未发布 1已发布）
     */
    private String publishStatus;

    /**
     * 发布地址
     */
    private String publishUrl;

    /**
     * 发布时间
     */
    private Date publishTime;
}
