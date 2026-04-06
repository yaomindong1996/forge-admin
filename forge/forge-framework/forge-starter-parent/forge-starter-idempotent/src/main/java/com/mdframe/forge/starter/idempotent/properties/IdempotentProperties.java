package com.mdframe.forge.starter.idempotent.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "forge.idempotent")
public class IdempotentProperties {
    
    private boolean enabled = true;
    
    private String prefix = "idempotent:";
    
    private int expire = 600;
    
    private String message = "请勿重复提交";
    
    private CacheProperties cache = new CacheProperties();
    
    private TokenProperties token = new TokenProperties();
    
    private LockProperties lock = new LockProperties();
}