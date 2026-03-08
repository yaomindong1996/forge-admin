package com.mdframe.forge.starter.id.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 配置文件处理器
 *
 * @author haoxd
 */
@EnableConfigurationProperties({UidCacheGeneratorProperties.class, UidDefaultGeneratorProperties.class})
@Configuration
public class IdServerPropertiesProcessor {

}
