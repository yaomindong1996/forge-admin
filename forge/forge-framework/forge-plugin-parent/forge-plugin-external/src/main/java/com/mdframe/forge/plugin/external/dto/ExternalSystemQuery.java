package com.mdframe.forge.plugin.external.dto;

import lombok.Data;

@Data
public class ExternalSystemQuery {

    private Integer pageNum = 1;
    
    private Integer pageSize = 10;
    
    private Long tenantId;
    
    private String systemCode;
    
    private String systemName;
    
    private String authType;
    
    private Integer systemStatus;
}