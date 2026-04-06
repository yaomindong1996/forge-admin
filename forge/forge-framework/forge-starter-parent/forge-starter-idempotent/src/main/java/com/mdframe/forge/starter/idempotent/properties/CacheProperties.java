package com.mdframe.forge.starter.idempotent.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "forge.idempotent.cache")
public class CacheProperties {
    
    private boolean enabled = true;
    
    private int expire = 3600;
    
    private int maxSize = 10000;
}