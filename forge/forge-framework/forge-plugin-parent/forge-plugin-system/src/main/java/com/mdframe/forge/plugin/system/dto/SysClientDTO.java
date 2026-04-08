package com.mdframe.forge.plugin.system.dto;

import lombok.Data;
import java.util.List;

@Data
public class SysClientDTO {
    private Long id;
    private String clientCode;
    private String clientName;
    private String appId;
    private String appSecret;
    
    private Long tokenTimeout;
    private Long tokenActivityTimeout;
    private String tokenPrefix;
    private String tokenName;
    private Boolean concurrentLogin;
    private Boolean shareToken;
    
    private Boolean enableIpLimit;
    private List<String> ipWhitelist;
    
    private Boolean enableEncrypt;
    private String encryptAlgorithm;
    
    private Long maxUserCount;
    private Long maxOnlineCount;
    private String authTypes;
    
    private Integer status;
    private String description;
    private Long tenantId;
}