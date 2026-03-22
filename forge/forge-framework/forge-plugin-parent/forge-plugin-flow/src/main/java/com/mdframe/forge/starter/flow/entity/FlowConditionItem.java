package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 条件规则项实体
 * 用于存储组合条件中的单个条件项
 *
 * @author forge
 */
@Data
@TableName("sys_flow_condition_item")
public class FlowConditionItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 规则ID
     */
    private String ruleId;

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 字段标签
     */
    private String fieldLabel;

    /**
     * 字段类型：string/number/date/boolean/user/dept/role
     */
    private String fieldType;

    /**
     * 操作符：eq/ne/gt/lt/ge/le/contains/startsWith/endsWith/in/notIn/isEmpty/isNotEmpty
     */
    private String operator;

    /**
     * 比较值（JSON格式，支持多值）
     */
    private String value;

    /**
     * 逻辑连接符：and/or
     */
    private String logicConnector;

    /**
     * 分组ID（用于条件分组）
     */
    private String groupId;

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