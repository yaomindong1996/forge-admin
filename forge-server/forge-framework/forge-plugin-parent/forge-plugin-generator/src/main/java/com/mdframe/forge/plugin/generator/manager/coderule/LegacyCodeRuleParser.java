package com.mdframe.forge.plugin.generator.manager.coderule;

import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSegmentDTO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将历史占位模板解释为结构化分段，供兼容读取和首次保存物化。
 */
@Component
public class LegacyCodeRuleParser {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern LEGACY_TOKEN_PATTERN = Pattern.compile("(?<!\\$)\\{([^{}]+)}");
    private static final Pattern SEQUENCE_PATTERN = Pattern.compile("seq(?::)?(\\d+)?", Pattern.CASE_INSENSITIVE);
    private static final Set<String> DATE_FORMATS = Set.of(
            "yyyy", "yyyyMM", "yyyyMMdd", "yyyyMMddHH", "yyyyMMddHHmm", "yyyyMMddHHmmss", "HHmmss"
    );
    private static final Set<String> SYSTEM_VARIABLES = Set.of(
            "tenantId", "userId", "username", "deptId", "orgId", "deptCode", "orgCode", "starter"
    );

    public List<CodeRuleSegmentDTO> parse(String template, String resetPolicy, Integer seqLength) {
        String normalized = normalizeTemplate(template);
        if (StringUtils.isBlank(normalized)) {
            throw new BusinessException("历史编码模板不能为空");
        }
        List<CodeRuleSegmentDTO> segments = new ArrayList<>();
        Map<String, Integer> counters = new HashMap<>();
        String effectiveResetPolicy = "AUTO".equalsIgnoreCase(resetPolicy)
                ? inferResetPolicy(normalized)
                : resetPolicy;
        Matcher matcher = TOKEN_PATTERN.matcher(normalized);
        int lastIndex = 0;
        while (matcher.find()) {
            addFixed(segments, counters, normalized.substring(lastIndex, matcher.start()));
            addToken(segments, counters, matcher.group(1), effectiveResetPolicy, seqLength);
            lastIndex = matcher.end();
        }
        addFixed(segments, counters, normalized.substring(lastIndex));
        if (segments.isEmpty()) {
            addFixed(segments, counters, normalized);
        }
        for (int index = 0; index < segments.size(); index++) {
            segments.get(index).setSegmentOrder(index + 1);
        }
        return segments;
    }

    private void addToken(List<CodeRuleSegmentDTO> segments,
                          Map<String, Integer> counters,
                          String rawToken,
                          String resetPolicy,
                          Integer seqLength) {
        String token = StringUtils.trimToEmpty(rawToken);
        Matcher sequenceMatcher = SEQUENCE_PATTERN.matcher(token);
        if (sequenceMatcher.matches()) {
            String lengthText = sequenceMatcher.group(1);
            int length = StringUtils.isBlank(lengthText) ? normalizeLength(seqLength) : Integer.parseInt(lengthText);
            CodeRuleSegmentDTO segment = baseSegment("SEQ", counters);
            segment.setSegmentLength(length);
            segment.setPadEnabled(1);
            segment.setPadChar("0");
            segment.setRadixType("DECIMAL");
            segment.setStartValue(1L);
            String normalizedReset = normalizeResetPolicy(resetPolicy);
            segment.setResetEnabled("NONE".equals(normalizedReset) ? 0 : 1);
            segment.setResetPolicy(normalizedReset);
            segments.add(segment);
            return;
        }
        if (DATE_FORMATS.contains(token)) {
            CodeRuleSegmentDTO segment = baseSegment("DATE", counters);
            segment.setSegmentValue(token);
            segment.setSegmentLength(token.length());
            segments.add(segment);
            return;
        }
        if (token.startsWith("field:")) {
            CodeRuleSegmentDTO segment = baseSegment("VARIABLE", counters);
            segment.setSegmentValue(StringUtils.trimToNull(token.substring("field:".length())));
            segments.add(segment);
            return;
        }
        if (SYSTEM_VARIABLES.contains(token)) {
            CodeRuleSegmentDTO segment = baseSegment("SYS_VAR", counters);
            segment.setSegmentValue(token);
            if ("orgCode".equals(token) || "deptCode".equals(token)) {
                segment.setGroupEnabled(1);
            }
            segments.add(segment);
            return;
        }
        CodeRuleSegmentDTO segment = baseSegment("VARIABLE", counters);
        segment.setSegmentValue(token);
        segments.add(segment);
    }

    private void addFixed(List<CodeRuleSegmentDTO> segments,
                          Map<String, Integer> counters,
                          String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        CodeRuleSegmentDTO segment = baseSegment("FIXED", counters);
        segment.setSegmentValue(value);
        segment.setSegmentLength(value.length());
        segments.add(segment);
    }

    private CodeRuleSegmentDTO baseSegment(String type, Map<String, Integer> counters) {
        int counter = counters.merge(type, 1, Integer::sum);
        CodeRuleSegmentDTO segment = new CodeRuleSegmentDTO();
        segment.setSegmentKey(type.toLowerCase(Locale.ROOT) + "_" + counter);
        segment.setSegmentType(type);
        segment.setPadEnabled(0);
        segment.setPadDirection("LEFT");
        segment.setGroupEnabled(0);
        segment.setIncludeInCode(1);
        segment.setResetEnabled(0);
        segment.setResetPolicy("NONE");
        segment.setStartValue(1L);
        segment.setExcludeAmbiguous(0);
        return segment;
    }

    private String normalizeTemplate(String template) {
        String value = StringUtils.trimToEmpty(template);
        Matcher matcher = LEGACY_TOKEN_PATTERN.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement("${" + matcher.group(1) + "}"));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String normalizeResetPolicy(String resetPolicy) {
        String value = StringUtils.defaultIfBlank(resetPolicy, "NONE").toUpperCase(Locale.ROOT);
        return switch (value) {
            case "SECOND", "MINUTE" -> "HOUR";
            case "YEAR", "MONTH", "DAY", "HOUR" -> value;
            default -> "NONE";
        };
    }

    private String inferResetPolicy(String template) {
        if (template.contains("yyyyMMddHH") || template.contains("HHmmss")) {
            return "HOUR";
        }
        if (template.contains("yyyyMMdd")) {
            return "DAY";
        }
        if (template.contains("yyyyMM")) {
            return "MONTH";
        }
        if (template.contains("yyyy")) {
            return "YEAR";
        }
        return "NONE";
    }

    private int normalizeLength(Integer seqLength) {
        return seqLength == null || seqLength < 1 ? 4 : Math.min(seqLength, 32);
    }
}
