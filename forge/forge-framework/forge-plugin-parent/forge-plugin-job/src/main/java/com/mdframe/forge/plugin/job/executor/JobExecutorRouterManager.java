package com.mdframe.forge.plugin.job.executor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 执行器路由管理器
 * 根据执行模式自动选择合适的路由器（本地/远程）
 */
@Slf4j
@Component
public class JobExecutorRouterManager {
    
    @Autowired
    private List<IJobExecutorRouter> routers;
    
    /**
     * 路由并执行任务
     */
    public String route(String executeMode,
                       String executorBean,
                       String executorMethod,
                       String executorHandler,
                       String executorService,
                       String jobParam) throws Exception {
        
        for (IJobExecutorRouter router : routers) {
            if (router.support(executeMode)) {
                log.debug("使用路由器: {} 执行模式: {}", router.getClass().getSimpleName(), executeMode);
                return router.route(executeMode, executorBean, executorMethod,
                                  executorHandler, executorService, jobParam);
            }
        }
        
        throw new UnsupportedOperationException("不支持的执行模式: " + executeMode);
    }
}
