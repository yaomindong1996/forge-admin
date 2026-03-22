package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 节点操作权限配置实体
 * 用于配置每个节点允许的操作
 *
 * @author forge
 */
@Data
@TableName("sys_flow_node_operation")
public class FlowNodeOperation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 节点配置ID
     */
    private String nodeConfigId;

    /**
     * 操作代码
     * approve-通过，reject-拒绝，transfer-转办，delegate-委派
     * addSign-加签，reduceSign-减签，withdraw-撤回，comment-评论
     */
    private String operationCode;

    /**
     * 操作名称
     */
    private String operationName;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 是否需要填写原因
     */
    private Boolean requiredReason;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}