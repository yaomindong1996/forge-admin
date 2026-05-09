package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程分类实体（树形结构）
 */
@Data
@TableName("sys_flow_category")
public class FlowCategory {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 父分类ID（根节点为空）
     */
    private String parentId;

    /**
     * 层级深度（1,2,3...）
     */
    private Integer level;

    /**
     * 祖先路径（如：0/abc123/）
     */
    private String ancestors;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态（0-禁用/1-启用）
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 子分类（用于树形查询）
     */
    @TableField(exist = false)
    private List<FlowCategory> children;
}