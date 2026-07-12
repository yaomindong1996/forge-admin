package com.mdframe.forge.plugin.capability.secureaction.publish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectDesignVersion;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从不可变发布模型快照提取可写字段，作为发布和调用阶段的共同安全边界。
 */
@RequiredArgsConstructor
public final class SecureActionPublishedModelPolicy {

    private final ObjectMapper objectMapper;

    public Map<String, LowcodeFieldSchema> writableFields(AiBusinessObjectDesignVersion version) {
        if (version == null || StringUtils.isBlank(version.getModelSnapshot())) {
            throw new BusinessException("发布版本缺少模型快照");
        }
        LowcodeModelSchema model;
        try {
            model = objectMapper.readValue(version.getModelSnapshot(), LowcodeModelSchema.class);
        }
        catch (Exception exception) {
            throw new BusinessException("发布版本模型快照无效");
        }
        Map<String, LowcodeFieldSchema> result = new LinkedHashMap<>();
        List<LowcodeFieldSchema> fields = model.getFields() == null ? List.of() : model.getFields();
        for (LowcodeFieldSchema field : fields) {
            if (field == null || Boolean.TRUE.equals(field.getSystemField())
                    || Boolean.TRUE.equals(field.getReadonly()) || Boolean.TRUE.equals(field.getPrimaryKey())
                    || StringUtils.isBlank(field.getField())) {
                continue;
            }
            result.put(field.getField(), field);
        }
        return Map.copyOf(result);
    }
}
