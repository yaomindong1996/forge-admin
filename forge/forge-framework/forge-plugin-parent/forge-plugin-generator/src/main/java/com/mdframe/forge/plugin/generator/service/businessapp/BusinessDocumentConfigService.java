package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessDocumentConfigDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessDocumentConfigMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentConfigVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 业务对象单据配置服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessDocumentConfigService {

    private static final Set<String> SYSTEM_FIELDS = Set.of(
            "id", "tenantId", "tenant_id", "createBy", "create_by", "createTime", "create_time",
            "createDept", "create_dept", "updateBy", "update_by", "updateTime", "update_time"
    );

    private final BusinessDocumentConfigMapper documentConfigMapper;
    private final BusinessObjectService objectService;
    private final AiCrudConfigMapper crudConfigMapper;
    private final ObjectMapper objectMapper;

    public BusinessDocumentConfigVO getConfig(Long objectId) {
        AiBusinessObject object = objectService.requireEntity(objectId);
        AiBusinessDocumentConfig config = documentConfigMapper.selectByObjectId(resolveTenantId(), objectId);
        if (config == null) {
            config = documentConfigMapper.selectByObjectCode(resolveTenantId(), object.getObjectCode());
        }
        if (config == null) {
            BusinessDocumentConfigVO vo = new BusinessDocumentConfigVO();
            vo.setObjectId(object.getId());
            vo.setSuiteCode(object.getSuiteCode());
            vo.setObjectCode(object.getObjectCode());
            vo.setConfigKey(object.getConfigKey());
            vo.setDocumentEnabled(false);
            vo.setDocumentName(object.getObjectName() + "单据");
            vo.setStatusMapping(defaultStatusMapping());
            return vo;
        }
        return toVO(config);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(Long objectId, BusinessDocumentConfigDTO dto) {
        if (dto == null) {
            throw new BusinessException("单据配置不能为空");
        }
        AiBusinessObject object = objectService.requireEntity(objectId);
        boolean enabled = Boolean.TRUE.equals(dto.getDocumentEnabled());
        Map<String, String> statusMapping = normalizeStatusMapping(dto.getStatusMapping());
        if (enabled) {
            validateRequiredField("单据状态字段", dto.getStatusField());
            validateObjectField(object, dto.getStatusField(), "单据状态字段");
            validateOptionalObjectField(object, dto.getStarterField(), "发起人字段");
            validateOptionalObjectField(object, dto.getOwnerField(), "负责人字段");
        }

        AiBusinessDocumentConfig config = documentConfigMapper.selectByObjectId(resolveTenantId(), objectId);
        if (config == null) {
            config = documentConfigMapper.selectByObjectCode(resolveTenantId(), object.getObjectCode());
        }
        if (config == null) {
            config = new AiBusinessDocumentConfig();
            config.setTenantId(resolveTenantId());
            config.setObjectId(object.getId());
            config.setSuiteCode(object.getSuiteCode());
            config.setObjectCode(object.getObjectCode());
        }
        config.setObjectId(object.getId());
        config.setSuiteCode(object.getSuiteCode());
        config.setObjectCode(object.getObjectCode());
        config.setConfigKey(object.getConfigKey());
        config.setDocumentEnabled(enabled ? 1 : 0);
        config.setDocumentName(StringUtils.defaultIfBlank(dto.getDocumentName(), object.getObjectName() + "单据"));
        config.setDocumentNoRule(StringUtils.trimToNull(dto.getDocumentNoRule()));
        config.setStatusField(StringUtils.trimToNull(dto.getStatusField()));
        config.setStarterField(StringUtils.trimToNull(dto.getStarterField()));
        config.setOwnerField(StringUtils.trimToNull(dto.getOwnerField()));
        config.setDefaultFlowKey(StringUtils.trimToNull(dto.getDefaultFlowKey()));
        config.setStatusMapping(writeJson(statusMapping, "单据状态映射"));
        config.setOptions(writeJson(dto.getOptions(), "单据扩展配置"));

        if (config.getId() == null) {
            documentConfigMapper.insert(config);
        } else {
            documentConfigMapper.updateById(config);
        }
    }

    public AiBusinessDocumentConfig selectEnabledByObjectCode(String objectCode) {
        return selectEnabledByObjectCode(resolveTenantId(), objectCode);
    }

    public AiBusinessDocumentConfig selectEnabledByObjectCode(Long tenantId, String objectCode) {
        if (StringUtils.isBlank(objectCode)) {
            return null;
        }
        AiBusinessDocumentConfig config = documentConfigMapper.selectByObjectCode(
                tenantId != null ? tenantId : resolveTenantId(), objectCode);
        if (config == null || !Integer.valueOf(1).equals(config.getDocumentEnabled())) {
            return null;
        }
        return config;
    }

    public BusinessDocumentConfigVO toVO(AiBusinessDocumentConfig config) {
        BusinessDocumentConfigVO vo = new BusinessDocumentConfigVO();
        vo.setId(config.getId());
        vo.setObjectId(config.getObjectId());
        vo.setSuiteCode(config.getSuiteCode());
        vo.setObjectCode(config.getObjectCode());
        vo.setConfigKey(config.getConfigKey());
        vo.setDocumentEnabled(Integer.valueOf(1).equals(config.getDocumentEnabled()));
        vo.setDocumentName(config.getDocumentName());
        vo.setDocumentNoRule(config.getDocumentNoRule());
        vo.setStatusField(config.getStatusField());
        vo.setStarterField(config.getStarterField());
        vo.setOwnerField(config.getOwnerField());
        vo.setDefaultFlowKey(config.getDefaultFlowKey());
        vo.setStatusMapping(readStringMap(config.getStatusMapping()));
        vo.setOptions(readObjectMap(config.getOptions()));
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }

    private void validateRequiredField(String label, String field) {
        if (StringUtils.isBlank(field)) {
            throw new BusinessException(label + "不能为空");
        }
    }

    private void validateOptionalObjectField(AiBusinessObject object, String field, String label) {
        if (StringUtils.isBlank(field)) {
            return;
        }
        validateObjectField(object, field, label);
    }

    private void validateObjectField(AiBusinessObject object, String field, String label) {
        Set<String> fields = collectObjectFields(object);
        if (!fields.contains(field)) {
            throw new BusinessException(label + "不存在: " + field);
        }
    }

    private Set<String> collectObjectFields(AiBusinessObject object) {
        Set<String> fields = new LinkedHashSet<>(SYSTEM_FIELDS);
        if (object == null || StringUtils.isBlank(object.getConfigKey())) {
            return fields;
        }
        AiCrudConfig config = crudConfigMapper.selectByConfigKey(resolveTenantId(), object.getConfigKey());
        if (config == null || StringUtils.isBlank(config.getModelSchema())) {
            return fields;
        }
        try {
            LowcodeModelSchema modelSchema = objectMapper.readValue(config.getModelSchema(), LowcodeModelSchema.class);
            if (modelSchema.getFields() == null) {
                return fields;
            }
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field == null) {
                    continue;
                }
                addFieldAlias(fields, field.getField());
                addFieldAlias(fields, field.getColumnName());
            }
        } catch (Exception e) {
            throw new BusinessException("读取业务对象字段失败: " + e.getMessage());
        }
        return fields;
    }

    private void addFieldAlias(Set<String> fields, String field) {
        if (StringUtils.isBlank(field)) {
            return;
        }
        fields.add(field);
        fields.add(snakeToCamel(field));
    }

    private Map<String, String> normalizeStatusMapping(Map<String, String> input) {
        Map<String, String> result = defaultStatusMapping();
        if (input == null) {
            return result;
        }
        input.forEach((key, value) -> {
            if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                result.put(key.trim(), value.trim());
            }
        });
        return result;
    }

    private Map<String, String> defaultStatusMapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("DRAFT", "DRAFT");
        mapping.put("SUBMITTED", "SUBMITTED");
        mapping.put("IN_PROCESS", "IN_PROCESS");
        mapping.put("APPROVED", "APPROVED");
        mapping.put("REJECTED", "REJECTED");
        mapping.put("CANCELED", "CANCELED");
        mapping.put("CLOSED", "CLOSED");
        return mapping;
    }

    private Map<String, String> readStringMap(String json) {
        if (StringUtils.isBlank(json)) {
            return defaultStatusMapping();
        }
        try {
            Map<String, String> value = objectMapper.readValue(json, new TypeReference<>() {});
            return normalizeStatusMapping(value);
        } catch (Exception e) {
            return defaultStatusMapping();
        }
    }

    private Map<String, Object> readObjectMap(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String writeJson(Object value, String label) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(label + "格式不正确");
        }
    }

    private String snakeToCamel(String value) {
        if (StringUtils.isBlank(value) || !value.contains("_")) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}
