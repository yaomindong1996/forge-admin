package com.mdframe.forge.admin;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * @date 2025/12/1
 */
@Configuration
@ConditionalOnProperty(name = "feature.enabled", havingValue = "true")
public class FeatureConfig implements CommandLineRunner {
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("Feature Config");
    }
    // 当数据库中feature.enabled=true时生效
    
}
