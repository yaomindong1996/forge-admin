package com.mdframe.forge.plugin.message.domain.dto;

import lombok.Data;

@Data
public class MessageManageQueryDTO {
    
    private String type;
    
    private String channel;
    
    private Integer status;
    
    private String startTime;
    
    private String endTime;
    
    private String keyword;
}