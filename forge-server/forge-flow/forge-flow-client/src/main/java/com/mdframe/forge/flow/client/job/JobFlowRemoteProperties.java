package com.mdframe.forge.flow.client.job;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 独立 Flow 服务的任务流程适配配置。
 */
@Data
@ConfigurationProperties(prefix = "forge.flow.job.remote")
public class JobFlowRemoteProperties {

    private boolean enabled;

    private String url;

    private String token;
}
