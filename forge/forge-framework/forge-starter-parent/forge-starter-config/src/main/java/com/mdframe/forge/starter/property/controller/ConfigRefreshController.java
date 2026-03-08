package com.mdframe.forge.starter.property.controller;

import com.mdframe.forge.starter.property.refresh.ConfigRefresher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置刷新REST接口
 * 提供手动刷新配置的能力
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.config", name = "enable-refresh-endpoint", havingValue = "true", matchIfMissing = true)
public class ConfigRefreshController {
    
    private final ConfigRefresher configRefresher;
    
    /**
     * 手动刷新配置
     */
    @PostMapping("/refresh")
    public Map<String, Object> refresh() {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean refreshed = configRefresher.refresh();
            result.put("success", true);
            result.put("refreshed", refreshed);
            result.put("message", refreshed ? "配置刷新成功" : "配置无变化");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "配置刷新失败: " + e.getMessage());
        }
        return result;
    }
}
