package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.crypto.Encryptor;
import com.mdframe.forge.starter.crypto.crypto.EncryptorFactory;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategy;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategyFactory;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeType;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态CRUD服务
 * 基于DynamicCrudRepository实现，支持配置驱动的通用CRUD操作
 * 
 * @author forge
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicCrudService {

    private final DynamicCrudRepository repository;
    private final AiCrudConfigService configService;
    private final ObjectMapper objectMapper;
    private final DictValueProvider dictValueProvider;
    private final DesensitizeStrategyFactory desensitizeStrategyFactory;
    private final EncryptorFactory encryptorFactory;

    // ==================== 查询操作 ====================

    /**
     * 分页查询
     */
    public Page<Map<String, Object>> selectPage(String configKey, PageQuery pageQuery, DynamicCrudQuery query) {
        // 1. 加载配置
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 2. 获取字段映射
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        
        // 3. 解析搜索配置
        Set<String> allowedSearchFields = DynamicQueryGenerator.extractFieldNames(config.getSearchSchema(), objectMapper);
        Map<String, String> searchTypeMap = DynamicQueryGenerator.extractSearchTypeMap(config.getSearchSchema(), objectMapper);
        
        // 4. 构建搜索条件
        Map<String, Object> searchParams = (query != null) ? query.getSearchParams() : null;
        
        // 5. 构建排序
        String orderBy = DynamicQueryGenerator.buildOrderByClause(
                pageQuery.getOrderByColumn(), pageQuery.getIsAsc(), columnMapping);
        
        // 6. 执行分页查询
        Page<Map<String, Object>> page = repository.selectPage(
                tableName,
                pageQuery.getPageNum(),
                pageQuery.getPageSize(),
                searchParams,
                allowedSearchFields,
                searchTypeMap,
                columnMapping,
                orderBy
        );
        
        // 7. 转换字段名为camelCase
        List<Map<String, Object>> camelCaseRecords = DynamicQueryGenerator.convertListToCamelCase(page.getRecords());
        
        // 8. 读取链路统一先解密，再做翻译和脱敏，避免密文参与展示处理
        applyDecrypt(camelCaseRecords, config.getEncryptConfig());
        applyDictTranslation(camelCaseRecords, config.getTransConfig());
        applyDesensitize(camelCaseRecords, config.getDesensitizeConfig());
        
        page.setRecords(camelCaseRecords);
        return page;
    }

    /**
     * 根据ID查询
     */
    public Map<String, Object> selectById(String configKey, Long id) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        Map<String, Object> record = repository.selectById(tableName, id);
        if (record == null) {
            return null;
        }
        
        // 转换为camelCase
        Map<String, Object> camelCaseRecord = DynamicQueryGenerator.convertMapToCamelCase(record);
        
        // 单条读取同样遵循“解密 -> 翻译 -> 脱敏”顺序
        applyDecrypt(Collections.singletonList(camelCaseRecord), config.getEncryptConfig());
        applyDictTranslation(Collections.singletonList(camelCaseRecord), config.getTransConfig());
        applyDesensitize(Collections.singletonList(camelCaseRecord), config.getDesensitizeConfig());
        
        return camelCaseRecord;
    }

    // ==================== 新增操作 ====================

    /**
     * 新增
     */
    public void insert(String configKey, Map<String, Object> data) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 获取字段映射
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        
        // 获取允许写入的字段
        Set<String> allowedFields = DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper);
        
        // 过滤并转换字段名
        Map<String, Object> filteredData = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (allowedFields.contains(entry.getKey())) {
                String columnName = columnMapping.getOrDefault(entry.getKey(), DynamicQueryGenerator.camelToSnake(entry.getKey()));
                filteredData.put(columnName, entry.getValue());
            }
        }
        
        if (filteredData.isEmpty()) {
            throw new BusinessException("没有可写入的字段");
        }
        
        // 应用加密
        applyEncrypt(filteredData, config.getEncryptConfig());
        
        // 执行插入
        repository.insert(tableName, filteredData);
    }

    // ==================== 更新操作 ====================

    /**
     * 更新
     */
    public void updateById(String configKey, Map<String, Object> data) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 获取ID
        Object idValue = data.get("id");
        if (idValue == null) {
            throw new BusinessException("更新操作缺少id");
        }
        Long id = Long.valueOf(idValue.toString());
        
        // 获取字段映射
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        
        // 获取允许写入的字段
        Set<String> allowedFields = DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper);
        
        // 过滤并转换字段名
        Map<String, Object> filteredData = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if ("id".equals(key) || "tenantId".equals(key) || "tenant_id".equals(key)) {
                continue;
            }
            if (allowedFields.contains(key)) {
                String columnName = columnMapping.getOrDefault(key, DynamicQueryGenerator.camelToSnake(key));
                filteredData.put(columnName, entry.getValue());
            }
        }
        
        if (filteredData.isEmpty()) {
            throw new BusinessException("没有可更新的字段");
        }
        
        // 应用加密
        applyEncrypt(filteredData, config.getEncryptConfig());
        
        // 执行更新
        repository.updateById(tableName, id, filteredData);
    }

    // ==================== 删除操作 ====================

    /**
     * 删除
     */
    public void deleteById(String configKey, Long id) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 判断是否逻辑删除
        boolean logicDelete = repository.hasDelFlag(tableName);
        
        // 执行删除
        repository.deleteById(tableName, id, logicDelete);
    }

    // ==================== 加解密处理 ====================

    /**
     * 应用加密（写入时）
     * encryptConfig格式示例：
     * {
     *   "phone": {"algorithm": "SM4"},
     *   "idCard": {"algorithm": "AES"},
     *   "email": {"algorithm": "SM4"}
     * }
     */
    private void applyEncrypt(Map<String, Object> data, String encryptConfigJson) {
        if (StringUtils.isBlank(encryptConfigJson) || data == null || data.isEmpty()) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(encryptConfigJson);
            if (!configNode.isObject()) return;

            for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                String fieldName = entry.getKey(); // camelCase字段名
                JsonNode ruleNode = entry.getValue();

                // 转换为snake_case（数据库列名）
                String snakeFieldName = DynamicQueryGenerator.camelToSnake(fieldName);

                if (!data.containsKey(snakeFieldName) || data.get(snakeFieldName) == null) {
                    continue;
                }

                // 获取加密算法配置
                String algorithm = ruleNode.has("algorithm") ? ruleNode.get("algorithm").asText() : "";
                if (StringUtils.isBlank(algorithm)) {
                    continue;
                }

                // 获取加密器
                Encryptor encryptor = encryptorFactory.getEncryptor(algorithm);
                if (encryptor == null) {
                    log.warn("[DynamicCrudService] 未找到加密器, algorithm={}", algorithm);
                    continue;
                }

                // 加密字段值
                Object value = data.get(snakeFieldName);
                if (value instanceof String) {
                    String plainText = (String) value;
                    if (StringUtils.isNotBlank(plainText)) {
                        String encryptedValue = encryptor.encrypt(plainText);
                        data.put(snakeFieldName, encryptedValue);
                        log.debug("[DynamicCrudService] 加密字段: {}, algorithm: {}", fieldName, algorithm);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 加密处理失败", e);
        }
    }

    /**
     * 应用解密（读取时）
     * encryptConfig格式示例：
     * {
     *   "phone": {"algorithm": "SM4"},
     *   "idCard": {"algorithm": "AES"},
     *   "email": {"algorithm": "SM4"}
     * }
     */
    private void applyDecrypt(List<Map<String, Object>> rows, String encryptConfigJson) {
        if (StringUtils.isBlank(encryptConfigJson) || rows == null || rows.isEmpty()) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(encryptConfigJson);
            if (!configNode.isObject()) return;

            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                    String fieldName = entry.getKey(); // camelCase字段名
                    JsonNode ruleNode = entry.getValue();

                    if (!row.containsKey(fieldName) || row.get(fieldName) == null) {
                        continue;
                    }

                    // 获取加密算法配置
                    String algorithm = ruleNode.has("algorithm") ? ruleNode.get("algorithm").asText() : "";
                    if (StringUtils.isBlank(algorithm)) {
                        continue;
                    }

                    // 获取加密器
                    Encryptor encryptor = encryptorFactory.getEncryptor(algorithm);
                    if (encryptor == null) {
                        log.warn("[DynamicCrudService] 未找到加密器, algorithm={}", algorithm);
                        continue;
                    }

                    // 解密字段值
                    Object value = row.get(fieldName);
                    if (value instanceof String) {
                        String cipherText = (String) value;
                        if (StringUtils.isNotBlank(cipherText)) {
                            try {
                                String decryptedValue = encryptor.decrypt(cipherText);
                                row.put(fieldName, decryptedValue);
                                log.debug("[DynamicCrudService] 解密字段: {}, algorithm: {}", fieldName, algorithm);
                            } catch (Exception decryptException) {
                                log.warn("[DynamicCrudService] 解密字段失败: {}, 可能是明文数据", fieldName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解密处理失败", e);
        }
    }

    // ==================== 脱敏处理 ====================

    /**
     * 应用字段脱敏
     */
    private void applyDesensitize(List<Map<String, Object>> rows, String desensitizeConfigJson) {
        if (StringUtils.isBlank(desensitizeConfigJson) || rows == null || rows.isEmpty()) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(desensitizeConfigJson);
            if (!configNode.isObject()) return;

            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                    String fieldName = entry.getKey(); // camelCase字段名
                    JsonNode ruleNode = entry.getValue();
                    if (!row.containsKey(fieldName) || row.get(fieldName) == null) continue;

                    String typeStr = ruleNode.has("type") ? ruleNode.get("type").asText("CUSTOM") : "CUSTOM";
                    DesensitizeType type = DesensitizeType.valueOf(typeStr);
                    DesensitizeStrategy strategy = desensitizeStrategyFactory.getStrategy(type);
                    if (strategy != null) {
                        String originalValue = String.valueOf(row.get(fieldName));
                        row.put(fieldName, strategy.desensitize(originalValue));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 脱敏处理失败", e);
        }
    }

    // ==================== 字典翻译 ====================

    /**
     * 应用字典翻译
     */
    private void applyDictTranslation(List<Map<String, Object>> rows, String transConfigJson) {
        if (StringUtils.isBlank(transConfigJson) || rows == null || rows.isEmpty() || dictValueProvider == null) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(transConfigJson);
            if (!configNode.isObject()) return;

            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                    String sourceField = entry.getKey(); // camelCase字段名
                    JsonNode ruleNode = entry.getValue();
                    if (!row.containsKey(sourceField) || row.get(sourceField) == null) continue;

                    String dictType = ruleNode.has("dictType") ? ruleNode.get("dictType").asText() : "";
                    String targetField = ruleNode.has("targetField") ? ruleNode.get("targetField").asText()
                            : sourceField + "Name";
                    if (StringUtils.isBlank(dictType)) continue;

                    String key = String.valueOf(row.get(sourceField));
                    String label = dictValueProvider.getLabel(dictType, key);
                    if (label != null) {
                        row.put(targetField, label);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 字典翻译失败", e);
        }
    }

    // ==================== 配置加载 ====================

    /**
     * 获取配置
     */
    private AiCrudConfig getConfig(String configKey) {
        AiCrudConfig config = configService.getByConfigKey(configKey);
        if (config == null || "1".equals(config.getStatus())) {
            throw new BusinessException("CRUD配置不存在或已停用: " + configKey);
        }
        if (!"CONFIG".equals(config.getMode())) {
            throw new BusinessException("该配置不是配置驱动模式: " + configKey);
        }
        return config;
    }
}
