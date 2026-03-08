package com.mdframe.forge.starter.crypto.keyexchange;

import cn.dev33.satoken.annotation.SaIgnore;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 密钥交换控制器
 * 提供公钥获取和密钥协商接口
 */
@Slf4j
@RestController
@RequestMapping("/crypto")
@RequiredArgsConstructor
@ApiPermissionIgnore
@IgnoreTenant
public class KeyExchangeController {

    private final KeyExchangeService keyExchangeService;

    /**
     * 获取RSA公钥
     * 前端在进行密钥协商前调用此接口获取服务端公钥
     */
    @GetMapping("/public-key")
    @SaIgnore
    public ResponseEntity<Map<String, Object>> getPublicKey() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", new PublicKeyResponse(keyExchangeService.getPublicKey(), "RSA"));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    /**
     * 密钥交换
     * 前端生成会话密钥，用RSA公钥加密后发送到此接口
     */
    @PostMapping("/exchange")
    public ResponseEntity<Map<String, Object>> exchangeKey(
            @RequestBody KeyExchangeRequest request,
            HttpServletRequest httpRequest) {

        // 从请求中获取会话标识（可以使用token、sessionId等）
        String sessionId = getSessionId(httpRequest);
        log.info("密钥交换请求: sessionId={}",
            sessionId != null && sessionId.length() > 20 ? sessionId.substring(0, 20) + "..." : sessionId);
        
        if (sessionId == null || sessionId.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("msg", "缺少会话标识");
            return ResponseEntity.badRequest().body(result);
        }

        boolean success = keyExchangeService.exchangeKey(sessionId, request.getEncryptedKey());
        
        Map<String, Object> result = new HashMap<>();
        if (success) {
            log.info("密钥交换成功: sessionId={}",
                sessionId.length() > 20 ? sessionId.substring(0, 20) + "..." : sessionId);
            result.put("code", 200);
            result.put("msg", "密钥交换成功");
        } else {
            result.put("code", 500);
            result.put("msg", "密钥交换失败");
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 从请求中获取会话标识
     */
    private String getSessionId(HttpServletRequest request) {
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
        
        // 最后使用 HTTP Session ID
        return request.getSession(true).getId();
    }
}
