package com.mdframe.forge.starter.idempotent.strategy;

import com.mdframe.forge.starter.idempotent.annotation.Idempotent;
import com.mdframe.forge.starter.idempotent.exception.TokenInvalidException;
import com.mdframe.forge.starter.idempotent.properties.TokenProperties;
import com.mdframe.forge.starter.idempotent.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;

@Slf4j
@RequiredArgsConstructor
public class TokenRequiredStrategyHandler implements IdempotentStrategyHandler {
    
    private final TokenService tokenService;
    private final TokenProperties tokenProperties;
    private final IdempotentStrategyHandler delegateHandler;
    
    @Override
    public Object handle(ProceedingJoinPoint joinPoint, Idempotent annotation, String idempotentKey) throws Throwable {
        String token = extractToken();
        String prefix = annotation.prefix();
        
        if (!tokenService.validateToken(token, prefix)) {
            log.warn("Token模式: Token验证失败, token={}, prefix={}", token, prefix);
            throw new TokenInvalidException("Token无效或已过期");
        }
        
        tokenService.consumeToken(token, prefix);
        log.debug("Token模式: Token验证成功并已消费, token={}", token);
        
        return delegateHandler.handle(joinPoint, annotation, idempotentKey);
    }
    
    private String extractToken() {
        org.aspectj.lang.ProceedingJoinPoint jp = null;
        try {
            org.springframework.web.context.request.RequestContextHolder currentRequest = 
                (org.springframework.web.context.request.RequestContextHolder) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            
            if (currentRequest != null) {
                javax.servlet.http.HttpServletRequest request = 
                    ((org.springframework.web.context.request.ServletRequestAttributes) currentRequest).getRequest();
                return request.getHeader(tokenProperties.getHeader());
            }
        } catch (Exception e) {
            log.warn("提取Token失败: {}", e.getMessage());
        }
        return null;
    }
}