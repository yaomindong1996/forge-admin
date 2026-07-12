package com.mdframe.forge.plugin.capability.highrisk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import org.apache.commons.lang3.StringUtils;

import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class HighRiskBusinessStateService {
    private final BusinessObjectActionService actionService;
    private final DynamicCrudService crudService;
    private final ObjectMapper objectMapper;

    public HighRiskBusinessStateService(BusinessObjectActionService actionService,
                                        DynamicCrudService crudService,
                                        ObjectMapper objectMapper) {
        this.actionService = actionService;
        this.crudService = crudService;
        this.objectMapper = objectMapper;
    }

    public String snapshot(SecureActionDescriptor descriptor, Map<String, Object> input) {
        Object rawRecordId = input.get("recordId");
        String recordId = rawRecordId == null ? null : StringUtils.trimToNull(String.valueOf(rawRecordId));
        if (recordId == null) {
            return digest(Map.of("mode", "CREATE"));
        }
        var published = actionService.resolvePublishedAction(
                descriptor.suiteCode(), descriptor.objectCode(), descriptor.actionCode(),
                descriptor.publishedObjectVersion());
        Map<String, Object> record = crudService.selectById(
                published.object().getConfigKey(), Long.valueOf(recordId));
        Map<String, Object> state = new TreeMap<>();
        state.put("recordId", recordId);
        for (String field : descriptor.allowedFields()) {
            state.put(field, record == null ? null : record.get(field));
        }
        return digest(state);
    }

    private String digest(Object value) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(value instanceof Map<?, ?> map
                    ? new TreeMap<>(toStringMap(map)) : value);
            return "sha256:" + java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        }
        catch (Exception exception) {
            throw new IllegalStateException("业务状态摘要失败", exception);
        }
    }

    private Map<String, Object> toStringMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }
}
