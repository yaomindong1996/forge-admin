package com.mdframe.forge.plugin.system.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SysClientVO {
    private Long id;
    private String clientCode;
    private String clientName;
    private String appId;
    private String appSecretMasked;
    
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
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}