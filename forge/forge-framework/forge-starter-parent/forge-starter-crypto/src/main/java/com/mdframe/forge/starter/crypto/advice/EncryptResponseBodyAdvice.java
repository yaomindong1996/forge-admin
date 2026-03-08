package com.mdframe.forge.starter.crypto.advice;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigInfo;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.crypto.crypto.Encryptor;
import com.mdframe.forge.starter.crypto.crypto.EncryptorFactory;
import com.mdframe.forge.starter.crypto.domain.EncryptedResponse;
import com.mdframe.forge.starter.crypto.keyexchange.SessionKeyStore;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应体加密处理
 */
@Slf4j
@RestControllerAdvice
public class EncryptResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final CryptoProperties properties;
    private final EncryptorFactory encryptorFactory;
    private final ObjectMapper objectMapper;
    private final SessionKeyStore sessionKeyStore;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final IApiConfigManager apiConfigManager;
    
    
    public EncryptResponseBodyAdvice(CryptoProperties properties,
                                     EncryptorFactory encryptorFactory,
                                     ObjectMapper objectMapper,
                                     SessionKeyStore sessionKeyStore,
            IApiConfigManager apiConfigManager) {
        this.properties = properties;
        this.encryptorFactory = encryptorFactory;
        this.objectMapper = objectMapper;
        this.sessionKeyStore = sessionKeyStore;
        this.apiConfigManager = apiConfigManager;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // 未启用加密功能
        if (!Boolean.TRUE.equals(properties.getEnabled())
                || !Boolean.TRUE.equals(properties.getEnableApiCrypto())) {
            return false;
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false;
        }
        HttpServletRequest request = attributes.getRequest();
        ApiConfigInfo apiConfig = apiConfigManager.getApiConfig(request.getRequestURI(), request.getMethod());
        if (apiConfig != null) {
            return apiConfig.getNeedEncrypt();
        } else {
            // 检查方法或类上是否有@ApiDecrypt注解
            return AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ApiEncrypt.class)
                    || returnType.hasMethodAnnotation(ApiEncrypt.class);
        }
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        // 排除路径检查
        String path = request.getURI().getPath();
        if (isExcludePath(path)) {
            return body;
        }

        try {
            // 获取注解
            ApiEncrypt annotation = returnType.getMethodAnnotation(ApiEncrypt.class);
            if (annotation == null) {
                annotation = AnnotatedElementUtils.getMergedAnnotation(
                        returnType.getContainingClass(), ApiEncrypt.class);
            }

            // 确定使用的算法
            String algorithm = annotation != null && StrUtil.isNotBlank(annotation.algorithm())
                    ? annotation.algorithm()
                    : properties.getAlgorithm();

            // 获取加密器
            Encryptor encryptor = encryptorFactory.getEncryptor(algorithm);

            // 将响应体转为JSON字符串
            String jsonBody = objectMapper.writeValueAsString(body);

            // 获取动态密钥
            String sessionKey = getSessionKey(request);
            
            // 加密（优先使用动态密钥）
            String encryptedData;
            if (sessionKey != null) {
                encryptedData = encryptor.encrypt(jsonBody, sessionKey);
                log.debug("响应加密成功(动态密钥), 算法: {}", algorithm);
            } else {
                encryptedData = encryptor.encrypt(jsonBody);
                log.debug("响应加密成功(默认密钥), 算法: {}", algorithm);
            }

            // 返回加密后的数据结构
            return new EncryptedResponse(encryptedData, algorithm);

        } catch (Exception e) {
            log.error("响应加密失败", e);
            return body;
        }
    }

    /**
     * 从请求获取会话密钥
     */
    private String getSessionKey(ServerHttpRequest request) {
        if (sessionKeyStore == null) {
            log.debug("SessionKeyStore 未注入，使用默认密钥");
            return null;
        }
        if (!Boolean.TRUE.equals(properties.getEnableDynamicKey())) {
            log.debug("动态密钥未启用，使用默认密钥");
            return null;
        }
        
        try {
            if (request instanceof ServletServerHttpRequest servletRequest) {
                HttpServletRequest httpRequest = servletRequest.getServletRequest();
                String sessionId = getSessionIdFromRequest(httpRequest);
                
                if (sessionId != null) {
                    String key = sessionKeyStore.getKey(sessionId);
                    log.debug("获取会话密钥: sessionId={}, found={}",
                        sessionId.length() > 20 ? sessionId.substring(0, 20) + "..." : sessionId,
                        key != null);
                    return key;
                } else {
                    log.debug("未找到会话标识（Authorization 或 X-Session-Id）");
                }
            }
        } catch (Exception e) {
            log.warn("获取会话密钥失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从请求中获取会话标识
     */
    private String getSessionIdFromRequest(HttpServletRequest request) {
        // 优先从 Authorization header 获取 token
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        
        // 其次尝试从 X-Session-Id header 获取
        String sessionId = request.getHeader("X-Session-Id");
        if (sessionId != null && !sessionId.isEmpty()) {
            return sessionId;
        }
        
        // 最后使用 HTTP Session ID（与 KeyExchangeController 保持一致）
        return request.getSession(false) != null ? request.getSession(false).getId() : null;
    }

    private boolean isExcludePath(String path) {
        if (properties.getExcludePaths() == null || properties.getExcludePaths().isEmpty()) {
            return false;
        }
        for (String pattern : properties.getExcludePaths()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
