package com.mdframe.forge.starter.idempotent.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "forge.idempotent.token")
public class TokenProperties {
    
    private boolean enabled = true;
    
    private int expire = 300;
    
    private String header = "X-Idempotent-Token";
}