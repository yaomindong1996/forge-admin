package com.mdframe.forge.plugin.message.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReceiverVO {
    
    private Long userId;
    
    private String userName;
    
    private String orgName;
    
    private Integer readFlag;
    
    private LocalDateTime readTime;
}