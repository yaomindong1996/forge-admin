package com.mdframe.forge.starter.idempotent.aop;

import cn.hutool.core.util.IdUtil;
import com.mdframe.forge.starter.idempotent.annotation.Idempotent;
import com.mdframe.forge.starter.idempotent.enums.IdempotentStrategy;
import com.mdframe.forge.starter.idempotent.generator.IdempotentKeyGenerator;
import com.mdframe.forge.starter.idempotent.properties.IdempotentProperties;
import com.mdframe.forge.starter.idempotent.strategy.IdempotentStrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Map;

@Slf4j
@Aspect
public class IdempotentAspect {
    
    private final IdempotentKeyGenerator keyGenerator;
    private final IdempotentProperties properties;
    private final Map<IdempotentStrategy, IdempotentStrategyHandler> strategyHandlers;
    
    public IdempotentAspect(
            IdempotentKeyGenerator keyGenerator,
            IdempotentProperties properties,
            Map<IdempotentStrategy, IdempotentStrategyHandler> strategyHandlers) {
        this.keyGenerator = keyGenerator;
        this.properties = properties;
        this.strategyHandlers = strategyHandlers;
    }
    
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        if (!properties.isEnabled()) {
            log.debug("幂等组件已关闭，跳过幂等校验");
            return joinPoint.proceed();
        }
        
        String prefix = idempotent.prefix().isEmpty() ? properties.getPrefix() : idempotent.prefix();
        String idempotentKey = keyGenerator.generate(joinPoint, prefix, idempotent.key());
        
        String requestId = IdUtil.fastSimpleUUID();
        log.debug("幂等校验开始, requestId={}, strategy={}, key={}", 
            requestId, idempotent.strategy(), idempotentKey);
        
        IdempotentStrategy strategy = idempotent.strategy();
        IdempotentStrategyHandler handler = strategyHandlers.get(strategy);
        
        if (handler == null) {
            log.warn("未找到策略处理器: strategy={}", strategy);
            return joinPoint.proceed();
        }
        
        long startTime = System.currentTimeMillis();
        try {
            Object result = handler.handle(joinPoint, idempotent, idempotentKey);
            long elapsed = System.currentTimeMillis() - startTime;
            log.debug("幂等校验成功, requestId={}, elapsed={}ms", requestId, elapsed);
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("幂等校验失败, requestId={}, elapsed={}ms, error={}", 
                requestId, elapsed, e.getMessage(), e);
            throw e;
        }
    }
}