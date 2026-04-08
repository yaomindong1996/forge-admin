package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.core.domain.BaseEntity;
import com.mdframe.forge.starter.crypto.handler.EncryptTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_client", autoResultMap = true)
public class SysClient extends BaseEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private String clientCode;
    private String clientName;
    private String appId;
    
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String appSecret;
    
    private Long tokenTimeout;
    private Long tokenActivityTimeout;
    private String tokenPrefix;
    private String tokenName;
    private Boolean concurrentLogin;
    private Boolean shareToken;
    
    private Boolean enableIpLimit;
    
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
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