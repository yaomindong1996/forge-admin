package com.mdframe.forge.plugin.ai.multimodal.image;

import com.mdframe.forge.plugin.ai.agent.engine.context.AgentExecutionTaskDecorator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

/** 复用已验证的可信身份/租户/DS/MDC 传递，任务只捕获生成记录 ID。 */
@Component
public class AiImageTaskExecutor {
    private final AsyncTaskExecutor executor;
    private final AgentExecutionTaskDecorator decorator;

    public AiImageTaskExecutor(@Qualifier("forgeAiImageTaskExecutor") AsyncTaskExecutor executor,
                               AgentExecutionTaskDecorator decorator) {
        this.executor = executor;
        this.decorator = decorator;
    }

    public void execute(Runnable task) {
        executor.execute(decorator.decorate(task));
    }
}
