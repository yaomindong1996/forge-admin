package com.mdframe.forge.starter.id.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * UidDefaultGeneratorProperties参数配置类
 *
 * @author haoxd
 */
@Data
@ConfigurationProperties(prefix = UidDefaultGeneratorProperties.PREFIX)
public class UidDefaultGeneratorProperties {

    public static final String PREFIX = "uid.default";


    private int timeBits = 23;

    private int workerBits = 31;

    private int seqBits = 9;

    private String epochStr = "2021-04-07";



}
