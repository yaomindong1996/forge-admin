package com.mdframe.forge.plugin.job.executor.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.mdframe.forge.plugin.job.executor.IJobExecutor;
import com.mdframe.forge.plugin.job.executor.IJobExecutorRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 本地执行器路由（单体模式）
 * 直接在本地JVM中调用Bean方法或Handler
 */
@Slf4j
@Component
public class LocalJobExecutorRouter implements IJobExecutorRouter {
    
    @Override
    public String route(String executeMode,
                       String executorBean,
                       String executorMethod,
                       String executorHandler,
                       String executorService,
                       String jobParam) throws Exception {
        
        if ("BEAN".equals(executeMode)) {
            return executeBeanMode(executorBean, executorMethod, jobParam);
        } else if ("HANDLER".equals(executeMode)) {
            return executeHandlerMode(executorHandler, jobParam);
        } else {
            throw new UnsupportedOperationException("本地路由不支持执行模式: " + executeMode);
        }
    }
    
    @Override
    public boolean support(String executeMode) {
        return "BEAN".equals(executeMode) || "HANDLER".equals(executeMode);
    }
    
    /**
     * Bean模式执行
     */
    private String executeBeanMode(String beanName, String methodName, String param) throws Exception {
        if (beanName == null || methodName == null) {
            throw new RuntimeException("未指定执行器Bean或方法");
        }
        
        Object bean = SpringUtil.getBean(beanName);
        Method method = findMethod(bean.getClass(), methodName);
        if (method == null) {
            throw new RuntimeException("未找到执行方法: " + methodName);
        }
        
        method.setAccessible(true);
        Object result;
        if (method.getParameterCount() == 0) {
            result = method.invoke(bean);
        } else {
            result = method.invoke(bean, param);
        }
        
        return result != null ? result.toString() : "SUCCESS";
    }
    
    /**
     * Handler模式执行
     */
    private String executeHandlerMode(String handlerName, String param) throws Exception {
        IJobExecutor executor = SpringUtil.getBean(handlerName, IJobExecutor.class);
        return executor.execute(param);
    }
    
    /**
     * 查找指定名称的方法
     */
    private Method findMethod(Class<?> clazz, String methodName) {
        // 先尝试无参方法
        try {
            return clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            // 忽略
        }
        
        // 再尝试带String参数的方法
        try {
            return clazz.getDeclaredMethod(methodName, String.class);
        } catch (NoSuchMethodException e) {
            // 忽略
        }
        
        // 最后遍历所有方法查找
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        
        return null;
    }
}
