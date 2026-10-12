package com.mdframe.forge.plugin.ai.agent.engine.context;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("forge.agent.executor")
public class AgentExecutorProperties {

    @Min(1)
    private int corePoolSize = 4;
    @Min(1)
    private int maxPoolSize = 8;
    @Min(1)
    private int queueCapacity = 32;

    @AssertTrue(message = "Agent 最大线程数不能小于核心线程数")
    public boolean isPoolSizeValid() {
        return maxPoolSize >= corePoolSize;
    }
}
