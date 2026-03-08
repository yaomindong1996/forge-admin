package com.mdframe.forge.starter.id.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * UidCacheGeneratorProperties参数配置类
 *
 * @author haoxd
 */
@Data
@ConfigurationProperties(prefix = UidCacheGeneratorProperties.PREFIX)
public class UidCacheGeneratorProperties {

    public static final String PREFIX = "uid.cache";


    private int timeBits = 30;

    private int workerBits = 27;

    private int seqBits = 6;

    private String epochStr = "2021-04-07";



}
