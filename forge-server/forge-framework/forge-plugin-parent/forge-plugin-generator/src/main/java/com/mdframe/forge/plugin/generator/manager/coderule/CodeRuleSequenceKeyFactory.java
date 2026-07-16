package com.mdframe.forge.plugin.generator.manager.coderule;

import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSegmentDTO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 构造长度受控且不暴露原始分组数据的数据库序列键。
 */
@Component
public class CodeRuleSequenceKeyFactory {

    public SequenceKey build(CodeRuleDefinition definition,
                             CodeRuleSegmentDTO sequenceSegment,
                             LinkedHashMap<String, String> groupValues,
                             LocalDateTime now) {
        if (definition.getTenantId() == null || definition.getTenantId() <= 0) {
            throw new BusinessException("缺少有效租户上下文，不能生成编码");
        }
        String ruleIdentity = definition.getRuleId() == null
                ? digest(StringUtils.defaultString(definition.getRuleCode())).substring(0, 12)
                : String.valueOf(definition.getRuleId());
        String segmentHash = digest(StringUtils.defaultString(sequenceSegment.getSegmentKey())).substring(0, 8);
        String groupHash = groupValues.isEmpty() ? "global" : groupHash(groupValues).substring(0, 12);
        String period = resolvePeriod(sequenceSegment, now);
        String key = "cr:" + definition.getTenantId() + ":" + ruleIdentity + ":" + segmentHash
                + ":" + groupHash + ":" + period;
        if (key.length() > 100) {
            throw new BusinessException("编码规则生成的计数键超过数据库长度限制");
        }
        return new SequenceKey(key, groupHash, period);
    }

    public String groupHash(Map<String, String> groupValues) {
        StringBuilder canonical = new StringBuilder();
        groupValues.forEach((key, value) -> {
            String safeKey = StringUtils.defaultString(key);
            String safeValue = StringUtils.defaultString(value);
            canonical.append(safeKey.length()).append(':').append(safeKey)
                    .append(safeValue.length()).append(':').append(safeValue);
        });
        return digest(canonical.toString());
    }

    public String legacyKeyPrefix(CodeRuleDefinition definition) {
        if (definition.getTenantId() == null || definition.getTenantId() <= 0) {
            throw new BusinessException("缺少有效租户上下文，不能构造旧序列键前缀");
        }
        if (StringUtils.isBlank(definition.getRuleCode())) {
            throw new BusinessException("规则编码不能为空，不能构造旧序列键前缀");
        }
        return "code-rule:" + definition.getTenantId() + ":" + definition.getRuleCode() + ":";
    }

    private String resolvePeriod(CodeRuleSegmentDTO segment, LocalDateTime now) {
        if (!Integer.valueOf(1).equals(segment.getResetEnabled())) {
            return "all";
        }
        String policy = StringUtils.defaultIfBlank(segment.getResetPolicy(), "NONE").toUpperCase(Locale.ROOT);
        return switch (policy) {
            case "YEAR" -> now.format(DateTimeFormatter.ofPattern("yyyy"));
            case "MONTH" -> now.format(DateTimeFormatter.ofPattern("yyyyMM"));
            case "DAY" -> now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            case "HOUR" -> now.format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
            case "NONE" -> "all";
            default -> throw new BusinessException("不支持的流水重置周期: " + policy);
        };
    }

    private String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("无法构造编码规则分组摘要", e);
        }
    }

    public record SequenceKey(String key, String groupHash, String period) {
    }
}
