package com.mdframe.forge.plugin.message.domain.dto;

import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * 消息发送请求DTO
 */
@Data
public class MessageSendRequestDTO {
    
    /**
     * 消息标题
     */
    private String title;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 模板编码（如果使用模板）
     */
    private String templateCode;
    
    /**
     * 模板参数（用于渲染模板）
     */
    private Map<String, Object> params;
    
    /**
     * 指定接收人用户ID集合
     */
    private Set<Long> userIds;
    
    /**
     * 指定接收人组织ID集合
     */
    private Set<Long> orgIds;
    
    /**
     * 指定接收人租户ID集合
     */
    private Set<Long> tenantIds;
    
    /**
     * 发送范围：ALL-全员/ORG-指定组织/USERS-指定人员
     */
    private String sendScope;
    
    /**
     * 发送渠道：WEB/SMS/EMAIL/PUSH
     */
    private String channel;
    
    /**
     * 消息类型：SYSTEM/SMS/EMAIL/CUSTOM
     */
    private String type;
    
    /**
     * 业务类型（如：ORDER、APPROVAL、TASK等）
     */
    private String bizType;
    
    /**
     * 业务主键（如：订单ID、流程实例ID等）
     */
    private String bizKey;
}
