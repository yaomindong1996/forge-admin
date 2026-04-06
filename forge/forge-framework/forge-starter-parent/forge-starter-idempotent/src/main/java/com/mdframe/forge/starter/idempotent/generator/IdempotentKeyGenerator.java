package com.mdframe.forge.starter.idempotent.generator;

import org.aspectj.lang.ProceedingJoinPoint;

public interface IdempotentKeyGenerator {
    String generate(ProceedingJoinPoint joinPoint, String prefix, String key);
}
