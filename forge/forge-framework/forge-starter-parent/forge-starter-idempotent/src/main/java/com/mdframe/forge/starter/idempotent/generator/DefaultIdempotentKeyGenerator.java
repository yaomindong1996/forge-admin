package com.mdframe.forge.starter.idempotent.generator;

import cn.hutool.crypto.digest.DigestUtil;
import com.mdframe.forge.starter.idempotent.util.SpelUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public class DefaultIdempotentKeyGenerator implements IdempotentKeyGenerator {
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new LocalVariableTableParameterNameDiscoverer();

    @Override
    public String generate(ProceedingJoinPoint joinPoint, String prefix, String key) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);

        String keyValue;
        if (key != null && !key.isEmpty()) {
            Object spelResult = SpelUtil.parse(key, args, paramNames);
            keyValue = spelResult != null ? spelResult.toString() : "";
        } else {
            String methodSign = method.getDeclaringClass().getName() + ":" + method.getName();
            String argsStr = args != null ? Arrays.stream(args).map(arg -> arg != null ? arg.toString() : "null").collect(Collectors.joining(",")) : "";
            keyValue = DigestUtil.md5Hex(methodSign + ":" + argsStr);
        }
        return prefix + keyValue;
    }
}
