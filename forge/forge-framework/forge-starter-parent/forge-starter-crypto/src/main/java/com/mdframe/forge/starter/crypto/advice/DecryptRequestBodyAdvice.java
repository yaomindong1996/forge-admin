package com.mdframe.forge.starter.crypto.advice;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigInfo;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.crypto.crypto.Encryptor;
import com.mdframe.forge.starter.crypto.crypto.EncryptorFactory;
import com.mdframe.forge.starter.crypto.domain.EncryptedRequest;
import com.mdframe.forge.starter.crypto.keyexchange.SessionKeyStore;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * 请求体解密处理
 */
@Slf4j
@RestControllerAdvice
public class DecryptRequestBodyAdvice implements RequestBodyAdvice {

    private final CryptoProperties properties;
    private final EncryptorFactory encryptorFactory;
    private final ObjectMapper objectMapper;
    private final SessionKeyStore sessionKeyStore;
    private final IApiConfigManager apiConfigManager;

    public DecryptRequestBodyAdvice(CryptoProperties properties,
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
    public boolean supports(MethodParameter methodParameter,
                            Type targetType,
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
        // 内部服务调用（如 FlowClient）直接传输明文 JSON，无需解密
        if ("true".equalsIgnoreCase(request.getHeader("X-Inner-Call"))) {
            return false;
        }
        ApiConfigInfo apiConfig = apiConfigManager.getApiConfig(request.getRequestURI(), request.getMethod());
        if (apiConfig != null) {
            return apiConfig.getNeedEncrypt();
        } else {
            // 检查方法或类上是否有@ApiDecrypt注解
            return AnnotatedElementUtils.hasAnnotation(methodParameter.getContainingClass(), ApiDecrypt.class)
                    || methodParameter.hasMethodAnnotation(ApiDecrypt.class);
        }
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage,
                                           MethodParameter parameter,
                                           Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {
        try {
            // 读取请求体
            String encryptedBody = StreamUtils.copyToString(
                    inputMessage.getBody(), StandardCharsets.UTF_8);

            // 解析加密数据
            EncryptedRequest request = objectMapper.readValue(
                    encryptedBody, EncryptedRequest.class);

            // 获取注解
            ApiDecrypt annotation = parameter.getMethodAnnotation(ApiDecrypt.class);
            if (annotation == null) {
                annotation = AnnotatedElementUtils.getMergedAnnotation(
                        parameter.getContainingClass(), ApiDecrypt.class);
            }

            // 确定使用的算法
            String algorithm = annotation != null && StrUtil.isNotBlank(annotation.algorithm())
                    ? annotation.algorithm()
                    : (StrUtil.isNotBlank(request.getAlgorithm())
                    ? request.getAlgorithm()
                    : properties.getAlgorithm());

            // 获取解密器
            Encryptor encryptor = encryptorFactory.getEncryptor(algorithm);

            // 获取动态密钥
            String sessionKey = getSessionKey();
            
            // 解密（优先使用动态密钥）
            String decryptedData;
            if (sessionKey != null) {
                decryptedData = encryptor.decrypt(request.getData(), sessionKey);
                log.debug("请求解密成功(动态密钥), 算法: {}", algorithm);
            } else {
                decryptedData = encryptor.decrypt(request.getData());
                log.debug("请求解密成功(默认密钥), 算法: {}", algorithm);
            }

            // 返回解密后的输入流
            return new DecryptedHttpInputMessage(
                    inputMessage.getHeaders(),
                    new ByteArrayInputStream(decryptedData.getBytes(StandardCharsets.UTF_8))
            );

        } catch (Exception e) {
            log.error("请求解密失败", e);
            throw new IOException("请求解密失败", e);
        }
    }

    /**
     * 从当前请求获取会话密钥
     */
    private String getSessionKey() {
        if (sessionKeyStore == null) {
            log.debug("SessionKeyStore 未注入，使用默认密钥");
            return null;
        }
        if (!Boolean.TRUE.equals(properties.getEnableDynamicKey())) {
            log.debug("动态密钥未启用，使用默认密钥");
            return null;
        }
        
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                log.debug("RequestContextHolder.getRequestAttributes() 返回 null");
                return null;
            }
            
            HttpServletRequest request = attributes.getRequest();
            String sessionId = getSessionIdFromRequest(request);
            
            if (sessionId != null) {
                String key = sessionKeyStore.getKey(sessionId);
                log.debug("获取会话密钥: sessionId={}, found={}",
                    sessionId.length() > 20 ? sessionId.substring(0, 20) + "..." : sessionId,
                    key != null);
                return key;
            } else {
                log.debug("未找到会话标识（Authorization 或 X-Session-Id）");
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

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage,
                                MethodParameter parameter, Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage,
                                  MethodParameter parameter, Type targetType,
                                  Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    /**
     * 解密后的HttpInputMessage
     */
    private static class DecryptedHttpInputMessage implements HttpInputMessage {
        private final HttpHeaders headers;
        private final InputStream body;

        public DecryptedHttpInputMessage(HttpHeaders headers, InputStream body) {
            this.headers = headers;
            this.body = body;
        }

        @Override
        public InputStream getBody() {
            return body;
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
