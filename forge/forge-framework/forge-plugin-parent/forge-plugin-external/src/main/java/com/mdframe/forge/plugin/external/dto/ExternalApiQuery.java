package com.mdframe.forge.plugin.external.dto;

import lombok.Data;

@Data
public class ExternalApiQuery {

    private Integer pageNum = 1;
    
    private Integer pageSize = 10;
    
    private Long tenantId;
    
    private Long systemId;
    
    private String apiCode;
    
    private String apiName;
    
    private String apiMethod;
    
    private Integer apiStatus;
}