package com.mdframe.forge.plugin.message.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息列表VO
 */
@Data
public class MessageVO {
    
    /**
     * 消息ID
     */
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
     * 消息类型
     */
    private String type;
    
    /**
     * 发送渠道
     */
    private String sendChannel;
    
    /**
     * 已读标记：0-未读/1-已读
     */
    private Integer readFlag;
    
    /**
     * 阅读时间
     */
    private LocalDateTime readTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
