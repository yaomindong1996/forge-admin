package com.mdframe.forge.plugin.message.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.core.domain.BaseEntity;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统消息主表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class SysMessage extends TenantEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 消息标题
     */
    private String title;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型：SYSTEM-系统消息/SMS-短信/EMAIL-邮件/CUSTOM-自定义
     */
    private String type;
    
    /**
     * 发送范围：ALL-全员/ORG-指定组织/USERS-指定人员
     */
    private String sendScope;
    
    /**
     * 发送渠道：WEB-站内信/SMS-短信/EMAIL-邮件/PUSH-推送
     */
    private String sendChannel;
    
    /**
     * 消息状态：0-草稿/1-已发送/2-发送失败
     */
    private Integer status;
    
    /**
     * 发送人ID
     */
    private Long senderId;
    
    /**
     * 发送人姓名
     */
    private String senderName;
    
    /**
     * 模板编码
     */
    private String templateCode;
    
    /**
     * 模板参数JSON
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private java.util.Map<String, Object> templateParams;
    
    /**
     * 业务类型（如：ORDER、APPROVAL、TASK等）
     */
    private String bizType;
    
    /**
     * 业务主键（如：订单ID、流程实例ID等）
     */
    private String bizKey;
}
