package com.mdframe.forge.plugin.capability.flowaction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.flowaction.source.FlowActionSourceRow;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 外围系统业务申请创建服务。这里只接收经过能力版本 Schema 和字段白名单校验后的业务数据，
 * 申请人、归属人和初始单据状态始终由可信执行身份及发布配置补齐。
 */
@RequiredArgsConstructor
public class FlowApplicationSubmissionService {

    private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<>() { };

    private final DynamicCrudService dynamicCrudService;
    private final LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver;
    private final ObjectMapper objectMapper;

    public String create(
            FlowActionSourceRow source,
            ExecutionIdentity identity,
            Map<String, Object> businessData) {
        if (source == null || StringUtils.isBlank(source.getConfigKey())) {
            throw new BusinessException(409, "当前业务对象没有可写的低代码运行配置");
        }
        if (identity == null || identity.actorUserId() == null) {
            throw new BusinessException(403, "USER_DELEGATION_REQUIRED");
        }
        AiCrudConfig runtimeConfig;
        try {
            runtimeConfig = dynamicCrudService.getRuntimeConfig(source.getConfigKey());
        }
        catch (BusinessException exception) {
            throw new BusinessException(409, "低代码运行配置已失效，不能提交业务申请", exception);
        }
        if (!runtimeDataSourceResolver.resolve(runtimeConfig).isMaster()) {
            throw new BusinessException(409,
                    "当前业务对象使用外部运行数据源，暂不支持原子提交申请；请改用已有记录 START 或配置可靠消息扩展");
        }
        Map<String, Object> record = new LinkedHashMap<>(
                businessData == null ? Map.of() : businessData);
        putPlatformValue(record, source.getStarterField(), identity.actorUserId());
        putPlatformValue(record, source.getOwnerField(), identity.actorUserId());
        if (StringUtils.isNotBlank(source.getStatusField())) {
            putPlatformValue(record, source.getStatusField(), initialStatus(source.getStatusMapping()));
        }

        Map<String, Object> created = dynamicCrudService.insertInternal(source.getConfigKey(), record);
        Object recordId = created == null
                ? null : dynamicCrudService.resolveRecordId(source.getConfigKey(), created);
        String value = recordId == null ? null : String.valueOf(recordId).trim();
        if (value == null || !value.matches("^[1-9]\\d*$")) {
            throw new BusinessException(503, "业务申请创建成功但未返回有效记录主键");
        }
        return value;
    }

    private void putPlatformValue(Map<String, Object> record, String field, Object value) {
        String key = StringUtils.trimToNull(field);
        if (key != null && value != null) {
            record.put(key, value);
        }
    }

    private String initialStatus(String statusMapping) {
        if (StringUtils.isBlank(statusMapping)) {
            return "DRAFT";
        }
        try {
            Map<String, String> mapping = objectMapper.readValue(statusMapping, STRING_MAP_TYPE);
            return StringUtils.defaultIfBlank(mapping.get("DRAFT"), "DRAFT");
        }
        catch (Exception exception) {
            throw new BusinessException(409, "业务对象初始状态映射配置无效");
        }
    }
}
