package com.mdframe.forge.plugin.system.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.dto.CacheInfoDTO;
import com.mdframe.forge.starter.cache.service.ICacheService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.util.PageParamResolver;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 缓存管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/system/cache")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysCacheController {

    private static final List<String> SENSITIVE_CACHE_PREFIXES = List.of(
            "crypto:session:",
            "authorization:login:",
            "satoken:",
            "auth:sso:"
    );

    private final ICacheService cacheService;
    private final ObjectMapper objectMapper;

    @ModelAttribute
    public void assertPlatformAdmin() {
        SessionHelper.assertAdmin("只有超级管理员可以管理系统缓存");
    }

    /**
     * 分页查询缓存键列表
     */
    @GetMapping("/page")
    @OperationLog(module = "缓存管理", type = OperationType.QUERY, desc = "分页查询缓存列表")
    public RespInfo<Page<CacheInfoDTO>> page(
            @RequestParam(required = false, defaultValue = "*") String pattern,
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        int currentPage = PageParamResolver.resolve(page, pageNum);
        
        // 如果pattern为空或null，使用*匹配所有
        if (StrUtil.isBlank(pattern)) {
            pattern = "*";
        }
        
        // 获取并过滤敏感键后再分页，避免泄露 key 名称和 value 预览。
        List<String> allKeys = cacheService.keys(pattern).stream()
                .filter(key -> !isSensitiveCacheKey(key))
                .toList();
        long total = allKeys.size();
        int start = Math.max(0, (currentPage - 1) * pageSize);
        int end = Math.min(start + pageSize, allKeys.size());
        List<String> keys = start >= allKeys.size() ? List.of() : allKeys.subList(start, end);
        
        // 构建CacheInfoDTO列表
        List<CacheInfoDTO> records = new ArrayList<>();
        for (String key : keys) {
            CacheInfoDTO dto = new CacheInfoDTO();
            dto.setKey(key);
            
            // 获取类型
            String type = cacheService.getType(key);
            dto.setType(type);
            
            // 获取TTL
            long ttl = cacheService.getExpire(key);
            dto.setTtl(ttl);
            dto.setTtlDesc(formatTtl(ttl));
            
            // 获取值预览
            try {
                // 使用getCacheInfo获取值,避免直接调用get方法导致反序列化错误
                Map<String, Object> info = cacheService.getCacheInfo(key);
                Object value = info.get("value");
                String valueStr = convertToString(value);
                dto.setValuePreview(valueStr.length() > 100 ? valueStr.substring(0, 100) + "..." : valueStr);
            } catch (Exception e) {
                log.error("获取缓存值预览失败: key={}", key, e);
                dto.setValuePreview("获取失败: " + e.getMessage());
            }
            
            records.add(dto);
        }
        
        // 构建分页结果
        Page<CacheInfoDTO> pageResult = new Page<>(currentPage, pageSize, total);
        pageResult.setRecords(records);
        
        return RespInfo.success(pageResult);
    }

    /**
     * 获取缓存详细信息
     */
    @PostMapping("/getInfo")
    @OperationLog(module = "缓存管理", type = OperationType.QUERY, desc = "查询缓存详情")
    public RespInfo<CacheInfoDTO> getInfo(@RequestParam String key) {
        if (isSensitiveCacheKey(key)) {
            return RespInfo.error(403, "敏感缓存不允许通过管理接口读取");
        }
        Map<String, Object> cacheInfo = cacheService.getCacheInfo(key);
        
        CacheInfoDTO dto = new CacheInfoDTO();
        dto.setKey((String) cacheInfo.get("key"));
        dto.setType((String) cacheInfo.get("type"));
        dto.setTtl((Long) cacheInfo.get("ttl"));
        dto.setTtlDesc(formatTtl((Long) cacheInfo.get("ttl")));
        dto.setValue(cacheInfo.get("value"));
        
        // 设置值预览
        Object value = cacheInfo.get("value");
        String valueStr = convertToString(value);
        dto.setValuePreview(valueStr.length() > 100 ? valueStr.substring(0, 100) + "..." : valueStr);
        
        return RespInfo.success(dto);
    }

    /**
     * 删除单个缓存
     */
    @PostMapping("/remove")
    @OperationLog(module = "缓存管理", type = OperationType.DELETE, desc = "删除缓存")
    public RespInfo<Void> remove(@RequestParam String key) {
        if (isSensitiveCacheKey(key)) {
            return RespInfo.error(403, "敏感缓存不允许通过管理接口删除");
        }
        boolean result = cacheService.delete(key);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    /**
     * 批量删除缓存
     */
    @PostMapping("/removeBatch")
    @OperationLog(module = "缓存管理", type = OperationType.DELETE, desc = "批量删除缓存")
    public RespInfo<Void> removeBatch(@RequestBody String[] keys) {
        if (keys == null || keys.length == 0) {
            return RespInfo.error("请选择要删除的缓存");
        }
        if (Arrays.stream(keys).anyMatch(this::isSensitiveCacheKey)) {
            return RespInfo.error(403, "敏感缓存不允许通过管理接口删除");
        }
        long count = cacheService.delete(Arrays.asList(keys));
        return count > 0 ? RespInfo.success() : RespInfo.error("批量删除失败");
    }

    /**
     * 清空所有缓存
     */
    @PostMapping("/clear")
    @OperationLog(module = "缓存管理", type = OperationType.DELETE, desc = "清空缓存")
    public RespInfo<Void> clear(@RequestParam(required = false, defaultValue = "*") String pattern) {
        if (StrUtil.isBlank(pattern)) {
            pattern = "*";
        }
        if (isSensitiveCachePattern(pattern)) {
            return RespInfo.error(403, "敏感缓存不允许通过管理接口清理");
        }
        List<String> keys = cacheService.keys(pattern).stream()
                .filter(key -> !isSensitiveCacheKey(key))
                .toList();
        long count = keys.isEmpty() ? 0 : cacheService.delete(keys);
        return RespInfo.success("成功清除 " + count + " 个缓存", null);
    }

    /**
     * 获取Redis监控指标
     */
    @GetMapping("/metrics")
    @OperationLog(module = "缓存管理", type = OperationType.QUERY, desc = "查询缓存监控指标")
    public RespInfo<Map<String, Object>> metrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // 获取内存信息
        Map<String, Object> memoryInfo = cacheService.getMemoryInfo();
        metrics.put("memory", memoryInfo);
        
        // 获取统计信息（QPS等）
        Map<String, Object> statsInfo = cacheService.getStatsInfo();
        metrics.put("stats", statsInfo);
        
        // 获取服务器信息
        Map<String, Object> serverInfo = cacheService.getServerInfo();
        metrics.put("server", serverInfo);
        
        return RespInfo.success(metrics);
    }

    /**
     * 格式化TTL
     */
    private String formatTtl(Long ttl) {
        if (ttl == null) {
            return "未知";
        }
        if (ttl == -2) {
            return "Key不存在";
        }
        if (ttl == -1) {
            return "永不过期";
        }
        
        if (ttl < 60) {
            return ttl + "秒";
        } else if (ttl < 3600) {
            return (ttl / 60) + "分钟";
        } else if (ttl < 86400) {
            return (ttl / 3600) + "小时";
        } else {
            return (ttl / 86400) + "天";
        }
    }

    /**
     * 将对象转换为字符串
     */
    private String convertToString(Object value) {
        if (value == null) {
            return "";
        }
        
        if (value instanceof String) {
            return (String) value;
        }
        
        try {
            // 尝试转换为JSON字符串
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }

    private boolean isSensitiveCacheKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_CACHE_PREFIXES.stream().anyMatch(normalized::startsWith);
    }

    private boolean isSensitiveCachePattern(String pattern) {
        if (StrUtil.isBlank(pattern)) {
            return false;
        }
        String normalized = pattern.toLowerCase(Locale.ROOT)
                .replace("*", "")
                .replace("?", "");
        if (StrUtil.isBlank(normalized)) {
            return false;
        }
        return SENSITIVE_CACHE_PREFIXES.stream()
                .anyMatch(prefix -> normalized.startsWith(prefix) || prefix.startsWith(normalized));
    }
}
