package com.mdframe.forge.plugin.generator.service.printing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/** CRUD 已完成公式与安全处理；这里仅投影授权字段并对齐打印协议。 */
@Component
@RequiredArgsConstructor
public class LowcodePrintValueAdapter {
    private final LowcodePrintCatalogBuilder catalog;
    private final ObjectMapper json;

    public Map<String, Object> adapt(PrintMetadataResolver.Model model, Map<String, Object> row,
                                     String prefix, Set<String> requested) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (var field : model.schema().getFields()) {
            if (!requested.contains(prefix + "." + field.getField()) || !catalog.visible(field)) {
                continue;
            }
            Object value = row.get(field.getField());
            boolean masked = field.getSensitiveType() != null && !"NONE".equals(field.getSensitiveType());
            if (masked) {
                // 不能用 Name 翻译副本覆盖已脱敏值，且规则必须来自同一固定版本。
                try {
                    var rules = json.readTree(Objects.toString(model.config().getDesensitizeConfig(), "{}"));
                    if (!field.getSensitiveType().equals(rules.path(field.getField()).path("type").asText())) {
                        throw PrintFailure.field(prefix + "." + field.getField(), "敏感字段缺少匹配的脱敏规则");
                    }
                } catch (java.io.IOException ex) {
                    throw PrintFailure.field(prefix + "." + field.getField(), "敏感字段脱敏配置无效");
                }
            } else if (field.isSelectionLabelField() || (field.getDictType() != null && !field.getDictType().isBlank())) {
                String label = field.getField() + "Name";
                if (model.config().getTransConfig() != null) {
                    try {
                        label = json.readTree(model.config().getTransConfig()).path(field.getField())
                                .path("targetField").asText(label);
                    } catch (java.io.IOException ex) {
                        throw PrintFailure.field(prefix + "." + field.getField(), "字段翻译配置无效");
                    }
                }
                value = row.getOrDefault(label, value);
            }
            String type = catalog.type(field);
            if (value != null && "MONEY".equals(type)) {
                try {
                    value = new BigDecimal(value.toString()).movePointRight(2).toBigIntegerExact().toString();
                } catch (NumberFormatException | ArithmeticException ex) {
                    throw PrintFailure.field(prefix + "." + field.getField(), "打印金额无法精确转换为分");
                }
            } else if (value != null && "BOOLEAN".equals(type) && !(value instanceof Boolean)) {
                if (Set.of("0", "1", "true", "false").contains(value.toString())) {
                    value = Set.of("1", "true").contains(value.toString());
                } else {
                    throw PrintFailure.field(prefix + "." + field.getField(), "打印布尔字段值无效");
                }
            } else if (value instanceof Long || value instanceof BigInteger || value instanceof BigDecimal) {
                value = value.toString();
            } else if (value instanceof java.time.temporal.TemporalAccessor || value instanceof java.util.Date) {
                value = value.toString();
            } else if (value instanceof Collection<?> values && "TEXT".equals(type)) {
                value = values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining("、"));
            }
            result.put(field.getField(), value);
        }
        return result;
    }
}
