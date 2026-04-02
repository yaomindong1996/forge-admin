package com.mdframe.forge.plugin.message.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息业务类型配置表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message_biz_type")
public class SysMessageBizType extends TenantEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 业务类型编码
     */
    private String bizType;
    
    /**
     * 业务类型名称
     */
    private String bizName;
    
    /**
     * 跳转URL模板
     */
    private String jumpUrl;
    
    /**
     * 跳转方式：_self-当前页/_blank-新窗口
     */
    private String jumpTarget;
    
    /**
     * 图标
     */
    private String icon;
    
    /**
     * 排序
     */
    private Integer sort;
    
    /**
     * 是否启用：0-禁用/1-启用
     */
    private Integer enabled;
    
    /**
     * 备注说明
     */
    private String remark;
}