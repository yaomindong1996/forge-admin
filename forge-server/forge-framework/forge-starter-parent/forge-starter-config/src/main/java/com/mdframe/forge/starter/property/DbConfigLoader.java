package com.mdframe.forge.starter.property;

import com.mdframe.forge.starter.config.converter.ConfigConverter;
import com.mdframe.forge.starter.core.util.CryptoDeploymentSecretPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库配置统一加载器
 * <p>
 * 同时兼容两张配置表，加载时在内存中合并，不再双写：
 * <ul>
 *   <li>基础层：sys_config 中的散配置（config_type='Y'）</li>
 *   <li>覆盖层：sys_config_group 中启用分组的 JSON 实时拍平，同键时覆盖基础层，
 *       保证配置中心（分组）是唯一编辑入口，派生键值无需落库</li>
 * </ul>
 * 同时为含中划线的 key 生成驼峰变体，兼容两种绑定风格。
 */
public final class DbConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(DbConfigLoader.class);

    private static final ConfigConverter CONFIG_CONVERTER = new ConfigConverter();

    private DbConfigLoader() {
    }

    /**
     * 加载并合并数据库配置
     */
    public static Map<String, String> load(JdbcTemplate jdbcTemplate) {
        Map<String, String> result = new LinkedHashMap<>();
        result.putAll(loadSysConfig(jdbcTemplate));
        result.putAll(loadConfigGroups(jdbcTemplate));
        addCamelCaseVariants(result);
        log.info("数据库配置加载完成，共 {} 个配置项（含驼峰格式）", result.size());
        return result;
    }

    /**
     * 基础层：sys_config 散配置；失败时降级到旧 config_properties 表
     */
    private static Map<String, String> loadSysConfig(JdbcTemplate jdbcTemplate) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            String sql = "SELECT config_key, config_value FROM sys_config WHERE config_type = 'Y'";
            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
            data.forEach(row -> putIfPresent(result, row.get("config_key"), row.get("config_value")));
            log.info("从 sys_config 表加载了 {} 个配置项", result.size());
        } catch (Exception e) {
            log.warn("从 sys_config 表加载配置失败，尝试从 config_properties 表加载: {}", e.getMessage());
            try {
                String sql = "SELECT `key`, `value` FROM config_properties WHERE enabled = 1";
                List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
                data.forEach(row -> putIfPresent(result, row.get("key"), row.get("value")));
                log.info("从 config_properties 表加载了 {} 个配置项", result.size());
            } catch (Exception ex) {
                log.error("从数据库加载配置失败", ex);
            }
        }
        return result;
    }

    /**
     * 覆盖层：sys_config_group 启用分组的 JSON 实时拍平
     * 分组表不存在（未初始化或旧库）时跳过，不影响基础层
     */
    private static Map<String, String> loadConfigGroups(JdbcTemplate jdbcTemplate) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            String sql = "SELECT group_code, config_value FROM sys_config_group WHERE status = 1 ORDER BY sort ASC";
            List<Map<String, Object>> groups = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> group : groups) {
                Object groupCode = group.get("group_code");
                Object configValue = group.get("config_value");
                if (groupCode == null || configValue == null || configValue.toString().isBlank()) {
                    continue;
                }
                try {
                    Map<String, String> flattened =
                            CONFIG_CONVERTER.convertByGroupCode(groupCode.toString(), configValue.toString());
                    if (flattened != null) {
                        result.putAll(flattened);
                    }
                } catch (Exception ex) {
                    log.warn("配置分组[{}]JSON拍平失败，跳过该分组: {}", groupCode, ex.getMessage());
                }
            }
            log.info("从 sys_config_group 表拍平了 {} 个配置项", result.size());
        } catch (Exception e) {
            log.warn("从 sys_config_group 表加载配置失败（可能未初始化），跳过: {}", e.getMessage());
        }
        return result;
    }

    private static void putIfPresent(Map<String, String> result, Object keyObj, Object valueObj) {
        if (keyObj != null && valueObj != null) {
            String key = keyObj.toString();
            if (CryptoDeploymentSecretPolicy.isDeploymentSecretPropertyKey(key)) {
                log.warn("已忽略数据库中的部署级 crypto 密钥配置: {}", key);
                return;
            }
            result.put(key, valueObj.toString());
        }
    }

    /**
     * 为含中划线的 key 追加驼峰变体（例如 max-login-attempts -> maxLoginAttempts）
     */
    private static void addCamelCaseVariants(Map<String, String> result) {
        Map<String, String> variants = new HashMap<>();
        result.forEach((key, value) -> {
            String camelCaseKey = convertToCamelCase(key);
            if (!camelCaseKey.equals(key) && !result.containsKey(camelCaseKey)) {
                variants.put(camelCaseKey, value);
            }
        });
        result.putAll(variants);
    }

    private static String convertToCamelCase(String key) {
        if (key == null || !key.contains("-")) {
            return key;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : key.toCharArray()) {
            if (c == '-') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }
}
