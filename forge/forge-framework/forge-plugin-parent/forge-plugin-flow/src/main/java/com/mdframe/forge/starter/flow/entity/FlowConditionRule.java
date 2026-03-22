package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 流程条件规则实体
 * 用于存储流程流转条件的可视化配置
 *
 * @author forge
 */
@Data
@TableName("sys_flow_condition_rule")
public class FlowConditionRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * 序列流ID（BPMN中的SequenceFlow ID）
     */
    private String sequenceFlowId;

    /**
     * 条件类型：simple-简单条件，composite-组合条件，script-脚本
     */
    private String conditionType;

    /**
     * 条件表达式（JSON格式存储）
     * 包含字段、操作符、值等信息
     */
    private String conditionExpression;

    /**
     * 优先级（数字越小优先级越高）
     */
    private Integer priority;

    /**
     * 是否默认路径
     */
    private Boolean isDefault;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除标志
     */
    @TableLogic
    private Integer deleted;
}