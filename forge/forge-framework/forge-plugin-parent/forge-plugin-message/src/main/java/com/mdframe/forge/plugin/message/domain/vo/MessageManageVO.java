package com.mdframe.forge.plugin.message.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageManageVO {
    
    private Long id;
    
    private String title;
    
    private String type;
    
    private String channel;
    
    private Integer status;
    
    private Integer receiverCount;
    
    private Integer readCount;
    
    private Integer unreadCount;
    
    private LocalDateTime createTime;
    
    private String senderName;
}