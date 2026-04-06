package com.mdframe.forge.starter.idempotent.strategy;

import com.mdframe.forge.starter.idempotent.annotation.Idempotent;
import org.aspectj.lang.ProceedingJoinPoint;

public interface IdempotentStrategyHandler {
    
    Object handle(ProceedingJoinPoint joinPoint, Idempotent annotation, String idempotentKey) throws Throwable;
}