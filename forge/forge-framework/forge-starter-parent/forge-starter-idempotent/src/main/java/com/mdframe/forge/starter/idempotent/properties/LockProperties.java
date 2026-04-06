package com.mdframe.forge.starter.idempotent.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "forge.idempotent.lock")
public class LockProperties {
    
    private boolean enabled = true;
    
    private long waitTime = 3000;
    
    private long leaseTime = 5000;
}