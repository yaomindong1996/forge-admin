package com.mdframe.forge.plugin.generator.manager.coderule;

import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSegmentDTO;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRuleGenerateVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRulePreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.id.service.ISequenceService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 结构化编码规则校验、预览与生成引擎。
 */
@Component
public class CodeRuleEngine {

    private static final Set<String> SEGMENT_TYPES = Set.of("DATE", "FIXED", "SEQ", "VARIABLE", "SYS_VAR");
    private static final Set<String> DATE_FORMATS = Set.of(
            "yyyy", "yyyyMM", "yyyyMMdd", "yyyyMMddHH", "yyyyMMddHHmm", "yyyyMMddHHmmss", "HHmmss"
    );
    private static final Set<String> SYSTEM_VARIABLES = Set.of(
            "tenantId", "userId", "username", "deptId", "orgId", "deptCode", "orgCode", "starter"
    );
    private static final Set<String> RESET_POLICIES = Set.of("NONE", "YEAR", "MONTH", "DAY", "HOUR");
    private static final Set<String> RADIX_TYPES = Set.of(
            "DECIMAL", "HEX", "ALPHA_UPPER", "ALPHA_LOWER", "ALPHANUMERIC"
    );
    private static final Set<String> VARIABLE_SOURCES = Set.of("CUSTOM", "LOWCODE");
    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]{0,63}$");
    private static final int LEGACY_WIDTH_CACHE_MAX_SIZE = 2_048;
    private static final int MAX_SEGMENT_COUNT = 32;
    private static final int MAX_BUSINESS_FIELD_COUNT = 256;
    private static final int MAX_UNDECLARED_VARIABLE_LENGTH = 96;

    private final ISequenceService sequenceService;
    private final CodeRuleRadixCodec radixCodec;
    private final CodeRuleSequenceKeyFactory keyFactory;
    private final Clock clock;
    private final Map<LegacyWidthCacheKey, Integer> legacyWidthCache = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<LegacyWidthCacheKey, Integer> eldest) {
            return size() > LEGACY_WIDTH_CACHE_MAX_SIZE;
        }
    };

    @Autowired
    public CodeRuleEngine(ISequenceService sequenceService,
                          CodeRuleRadixCodec radixCodec,
                          CodeRuleSequenceKeyFactory keyFactory) {
        this(sequenceService, radixCodec, keyFactory, Clock.systemDefaultZone());
    }

    public CodeRuleEngine(ISequenceService sequenceService,
                          CodeRuleRadixCodec radixCodec,
                          CodeRuleSequenceKeyFactory keyFactory,
                          Clock clock) {
        this.sequenceService = sequenceService;
        this.radixCodec = radixCodec;
        this.keyFactory = keyFactory;
        this.clock = clock;
    }

    public List<String> validate(CodeRuleDefinition definition) {
        List<CodeRuleSegmentDTO> segments = orderedSegments(definition);
        if (segments.isEmpty()) {
            throw new BusinessException("编码规则至少需要一个分段");
        }
        if (segments.size() > MAX_SEGMENT_COUNT) {
            throw new BusinessException("一条编码规则最多只能包含" + MAX_SEGMENT_COUNT + "个分段");
        }
        Set<String> segmentKeys = new HashSet<>();
        int sequenceCount = 0;
        int includedCount = 0;
        int totalLength = 0;
        List<String> warnings = new ArrayList<>();
        for (int index = 0; index < segments.size(); index++) {
            CodeRuleSegmentDTO segment = segments.get(index);
            normalizeSegment(segment, index + 1);
            if (!segmentKeys.add(segment.getSegmentKey())) {
                throw new BusinessException("分段键不能重复: " + segment.getSegmentKey());
            }
            if ("SEQ".equals(segment.getSegmentType())) {
                sequenceCount++;
                validateSequence(segment);
            } else {
                validateValueSegment(segment);
            }
            if (Integer.valueOf(1).equals(segment.getIncludeInCode())) {
                includedCount++;
                totalLength += declaredLength(segment);
            } else if (Integer.valueOf(1).equals(segment.getGroupEnabled())) {
                warnings.add("分组段“" + segment.getSegmentKey() + "”未输出到编码，跨分组可能产生相同编号");
            }
            if (Integer.valueOf(1).equals(segment.getGroupEnabled()) && "SEQ".equals(segment.getSegmentType())) {
                throw new BusinessException("流水号段不能参与自身分组");
            }
        }
        if (sequenceCount > 1) {
            throw new BusinessException("一条编码规则最多只能包含一个流水号段");
        }
        if (sequenceCount == 0) {
            warnings.add("当前规则不含流水号段，重复输入会生成相同编码");
        }
        if (includedCount == 0) {
            throw new BusinessException("至少需要一个列入编码的分段");
        }
        if (totalLength > 96) {
            throw new BusinessException("编码声明总长度不能超过96个字符");
        }
        return warnings;
    }

    public CodeRulePreviewVO preview(CodeRuleDefinition definition,
                                     Map<String, Object> fields,
                                     Map<String, Object> trustedSystemVariables,
                                     Long sampleSequence) {
        return render(definition, fields, trustedSystemVariables, sampleSequence, true).preview();
    }

    public CodeRuleGenerateVO generate(CodeRuleDefinition definition,
                                       Map<String, Object> fields,
                                       Map<String, Object> trustedSystemVariables) {
        if (definition.getTenantId() == null || definition.getTenantId() <= 0) {
            throw new BusinessException("缺少有效租户上下文，不能生成编码");
        }
        Object trustedTenantId = safeMap(trustedSystemVariables).get("tenantId");
        if (trustedTenantId == null || !String.valueOf(definition.getTenantId()).equals(String.valueOf(trustedTenantId))) {
            throw new BusinessException("租户上下文与编码规则不一致");
        }
        RenderResult rendered = render(definition, fields, trustedSystemVariables, null, false);
        CodeRuleGenerateVO result = new CodeRuleGenerateVO();
        result.setCode(rendered.code());
        result.setSequence(rendered.sequence());
        result.setGroupKey(rendered.groupKey());
        result.setPeriod(rendered.period());
        return result;
    }

    private RenderResult render(CodeRuleDefinition definition,
                                Map<String, Object> fields,
                                Map<String, Object> trustedSystemVariables,
                                Long sampleSequence,
                                boolean previewMode) {
        List<String> validationWarnings = validate(definition);
        List<CodeRuleSegmentDTO> segments = orderedSegments(definition);
        LocalDateTime now = LocalDateTime.now(clock);
        Map<String, Object> safeFields = safeBusinessFields(fields);
        Map<String, Object> safeSystemVariables = safeMap(trustedSystemVariables);
        LinkedHashMap<String, String> resolvedValues = new LinkedHashMap<>();
        LinkedHashMap<String, String> groupValues = new LinkedHashMap<>();
        CodeRuleSegmentDTO sequenceSegment = null;

        CodeRulePreviewVO preview = previewMode ? new CodeRulePreviewVO() : null;
        if (previewMode) {
            preview.setTemplate(toCompatibilityTemplate(segments));
            preview.setFormatExpression(toFormatExpression(segments));
            validationWarnings.forEach(message -> preview.getWarnings().add(issue(null, message, null)));
        }

        for (CodeRuleSegmentDTO segment : segments) {
            if ("SEQ".equals(segment.getSegmentType())) {
                sequenceSegment = segment;
                continue;
            }
            String value = resolveValue(segment, safeFields, safeSystemVariables, now, previewMode, preview);
            resolvedValues.put(segment.getSegmentKey(), value);
            if (Integer.valueOf(1).equals(segment.getGroupEnabled())) {
                groupValues.put(segment.getSegmentKey(), value);
            }
        }
        if (!previewMode) {
            validateResolvedOutputBeforeSequence(segments, resolvedValues);
        }

        Long sequence = null;
        CodeRuleSequenceKeyFactory.SequenceKey sequenceKey = null;
        if (sequenceSegment != null) {
            long startValue = sequenceSegment.getStartValue() == null ? 1L : sequenceSegment.getStartValue();
            if (previewMode) {
                sequence = sampleSequence == null ? startValue : sampleSequence;
                sequenceKey = keyFactory.build(definition, sequenceSegment, groupValues, now);
            } else {
                if (definition.getRuleId() == null) {
                    throw new BusinessException("未保存的编码规则不能执行真实生成");
                }
                sequenceKey = keyFactory.build(definition, sequenceSegment, groupValues, now);
                boolean legacyCompatible = isLegacyCompatible(definition);
                String legacyKeyPrefix = legacyCompatible
                        ? keyFactory.legacyKeyPrefix(definition) : null;
                boolean excludeAmbiguous = Integer.valueOf(1)
                        .equals(sequenceSegment.getExcludeAmbiguous());
                int allocationStep = legacyCompatible ? 1_000 : radixCodec.recommendedAllocationStep(
                        sequenceSegment.getRadixType(),
                        sequenceSegment.getSegmentLength(),
                        excludeAmbiguous);
                long maxValue = legacyCompatible ? Long.MAX_VALUE : radixCodec.maxValue(
                        sequenceSegment.getRadixType(),
                        sequenceSegment.getSegmentLength(),
                        excludeAmbiguous);
                sequence = sequenceService.nextId(
                        sequenceKey.key(),
                        startValue,
                        legacyKeyPrefix,
                        sequenceKey.period(),
                        allocationStep,
                        maxValue
                );
            }
            String sequenceValue = encodeSequence(
                    definition, sequenceSegment, sequence, startValue, sequenceKey.period(), previewMode);
            resolvedValues.put(sequenceSegment.getSegmentKey(), sequenceValue);
        }

        StringBuilder code = new StringBuilder();
        for (CodeRuleSegmentDTO segment : segments) {
            String value = resolvedValues.getOrDefault(segment.getSegmentKey(), "");
            if (Integer.valueOf(1).equals(segment.getIncludeInCode())) {
                code.append(value);
            }
            if (previewMode) {
                preview.getSegmentPreviews().add(segmentPreview(segment, value));
            }
        }
        String renderedCode = code.toString();
        String groupKey = sequenceKey == null
                ? (groupValues.isEmpty() ? "global" : keyFactory.groupHash(groupValues).substring(0, 12))
                : sequenceKey.groupHash();
        String period = sequenceKey == null ? "all" : sequenceKey.period();
        if (previewMode) {
            preview.setPreviewCode(renderedCode);
            preview.setTotalLength(code.length());
            preview.setSequence(sequence);
            preview.setGroupKey(groupKey);
            preview.setPeriod(period);
            if (!renderedCode.matches("[A-Za-z0-9_\\-./]+")) {
                preview.getErrors().add(issue(
                        null,
                        "编码包含不允许的字符",
                        "仅允许字母、数字、短横线、下划线、点和斜线"));
            }
            if (code.length() > 96) {
                preview.getErrors().add(issue(
                        null,
                        "编码实际长度超过96个字符",
                        "请缩短固定值或变量长度"));
            }
            preview.setValid(preview.getErrors().isEmpty());
        } else {
            validateGeneratedCode(renderedCode);
        }
        return new RenderResult(preview, renderedCode, sequence, groupKey, period);
    }

    private void validateResolvedOutputBeforeSequence(List<CodeRuleSegmentDTO> segments,
                                                      Map<String, String> resolvedValues) {
        int projectedLength = 0;
        for (CodeRuleSegmentDTO segment : segments) {
            if (!Integer.valueOf(1).equals(segment.getIncludeInCode())) {
                continue;
            }
            if ("SEQ".equals(segment.getSegmentType())) {
                projectedLength += segment.getSegmentLength();
                continue;
            }
            String value = resolvedValues.getOrDefault(segment.getSegmentKey(), "");
            if (!value.matches("[A-Za-z0-9_\\-./]+")) {
                throw new BusinessException("编码包含不允许的字符");
            }
            projectedLength += value.length();
        }
        if (projectedLength > 96) {
            throw new BusinessException("编码实际长度超过96个字符");
        }
    }

    private void validateGeneratedCode(String code) {
        if (!code.matches("[A-Za-z0-9_\\-./]+")) {
            throw new BusinessException("编码包含不允许的字符");
        }
        if (code.length() > 96) {
            throw new BusinessException("编码实际长度超过96个字符");
        }
    }

    private String encodeSequence(CodeRuleDefinition definition,
                                  CodeRuleSegmentDTO sequenceSegment,
                                  long sequence,
                                  long startValue,
                                  String period,
                                  boolean previewMode) {
        String radixType = sequenceSegment.getRadixType();
        boolean excludeAmbiguous = Integer.valueOf(1).equals(sequenceSegment.getExcludeAmbiguous());
        int configuredLength = sequenceSegment.getSegmentLength();
        int requiredLength = radixCodec.requiredLength(sequence, radixType, excludeAmbiguous);
        if (previewMode || requiredLength <= configuredLength) {
            return radixCodec.encode(sequence, radixType, configuredLength, excludeAmbiguous);
        }
        if (!isLegacyCompatible(definition)) {
            return radixCodec.encode(sequence, radixType, configuredLength, excludeAmbiguous);
        }

        LegacyWidthCacheKey cacheKey = new LegacyWidthCacheKey(
                definition.getTenantId(),
                definition.getRuleId(),
                sequenceSegment.getSegmentKey(),
                period,
                radixType,
                configuredLength,
                excludeAmbiguous
        );
        int compatibleLength = resolveCompatibleLength(
                cacheKey, definition, startValue, period, radixType, configuredLength, excludeAmbiguous);
        return radixCodec.encode(sequence, radixType, compatibleLength, excludeAmbiguous);
    }

    private boolean isLegacyCompatible(CodeRuleDefinition definition) {
        return Integer.valueOf(1).equals(definition.getLegacyCompatEnabled());
    }

    private int resolveCompatibleLength(LegacyWidthCacheKey cacheKey,
                                        CodeRuleDefinition definition,
                                        long startValue,
                                        String period,
                                        String radixType,
                                        int configuredLength,
                                        boolean excludeAmbiguous) {
        synchronized (legacyWidthCache) {
            Integer cachedLength = legacyWidthCache.get(cacheKey);
            if (cachedLength != null) {
                return cachedLength;
            }
        }

        long legacyStartValue = sequenceService.resolveLegacyStartValue(
                startValue,
                keyFactory.legacyKeyPrefix(definition),
                period
        );
        int resolvedLength = Math.max(configuredLength,
                radixCodec.requiredLength(legacyStartValue, radixType, excludeAmbiguous));
        synchronized (legacyWidthCache) {
            Integer cachedLength = legacyWidthCache.get(cacheKey);
            if (cachedLength != null) {
                return cachedLength;
            }
            legacyWidthCache.put(cacheKey, resolvedLength);
            return resolvedLength;
        }
    }

    private String resolveValue(CodeRuleSegmentDTO segment,
                                Map<String, Object> fields,
                                Map<String, Object> systemVariables,
                                LocalDateTime now,
                                boolean previewMode,
                                CodeRulePreviewVO preview) {
        String raw = switch (segment.getSegmentType()) {
            case "DATE" -> now.format(DateTimeFormatter.ofPattern(segment.getSegmentValue()));
            case "FIXED" -> StringUtils.defaultString(segment.getSegmentValue());
            case "VARIABLE" -> resolveMapValue(segment, fields, previewMode, preview, "业务字段", true);
            case "SYS_VAR" -> resolveMapValue(segment, systemVariables, previewMode, preview, "系统变量", false);
            default -> throw new BusinessException("不支持的编码分段类型: " + segment.getSegmentType());
        };
        return applyLength(segment, raw);
    }

    private String resolveMapValue(CodeRuleSegmentDTO segment,
                                   Map<String, Object> values,
                                   boolean previewMode,
                                   CodeRulePreviewVO preview,
                                   String sourceName,
                                   boolean resolveFieldAliases) {
        Object value = readMapValue(values, segment.getSegmentValue(), resolveFieldAliases);
        if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
            return String.valueOf(value);
        }
        if (!previewMode) {
            throw new BusinessException(sourceName + "缺少值: " + segment.getSegmentValue());
        }
        String sample = sampleValue(segment);
        preview.getWarnings().add(issue(
                segment.getSegmentKey(),
                sourceName + "未提供示例值: " + segment.getSegmentValue(),
                "预览使用占位样例，真实生成时必须提供"
        ));
        return sample;
    }

    private Object readMapValue(Map<String, Object> values, String key, boolean resolveFieldAliases) {
        if (values.containsKey(key)) {
            return values.get(key);
        }
        if (!resolveFieldAliases) {
            return null;
        }
        String camelAlias = DynamicQueryGenerator.snakeToCamel(key);
        if (values.containsKey(camelAlias)) {
            return values.get(camelAlias);
        }
        String snakeAlias = DynamicQueryGenerator.camelToSnake(key);
        return values.get(snakeAlias);
    }

    private String applyLength(CodeRuleSegmentDTO segment, String value) {
        Integer length = segment.getSegmentLength();
        if (length == null || "DATE".equals(segment.getSegmentType())) {
            if (length == null
                    && ("VARIABLE".equals(segment.getSegmentType())
                    || "SYS_VAR".equals(segment.getSegmentType()))
                    && value.length() > MAX_UNDECLARED_VARIABLE_LENGTH) {
                throw new BusinessException(
                        "分段“" + segment.getSegmentKey() + "”的值不能超过"
                                + MAX_UNDECLARED_VARIABLE_LENGTH + "个字符");
            }
            return value;
        }
        if (value.length() > length) {
            throw new BusinessException("分段“" + segment.getSegmentKey() + "”的值超过声明长度" + length);
        }
        if (!Integer.valueOf(1).equals(segment.getPadEnabled()) || value.length() == length) {
            return value;
        }
        String pad = StringUtils.defaultIfBlank(segment.getPadChar(), "0");
        int count = length - value.length();
        String repeated = pad.repeat(count);
        return "RIGHT".equals(segment.getPadDirection()) ? value + repeated : repeated + value;
    }

    private void normalizeSegment(CodeRuleSegmentDTO segment, int order) {
        if (segment == null) {
            throw new BusinessException("编码分段不能为空");
        }
        segment.setSegmentOrder(order);
        segment.setSegmentType(StringUtils.upperCase(StringUtils.trimToEmpty(segment.getSegmentType())));
        segment.setSegmentKey(StringUtils.trimToNull(segment.getSegmentKey()));
        segment.setSegmentValue(StringUtils.trimToNull(segment.getSegmentValue()));
        segment.setVariableSource(StringUtils.upperCase(
                StringUtils.defaultIfBlank(segment.getVariableSource(), "CUSTOM")));
        segment.setPadEnabled(Integer.valueOf(1).equals(segment.getPadEnabled()) ? 1 : 0);
        segment.setPadDirection("RIGHT".equalsIgnoreCase(segment.getPadDirection()) ? "RIGHT" : "LEFT");
        segment.setGroupEnabled(Integer.valueOf(1).equals(segment.getGroupEnabled()) ? 1 : 0);
        segment.setIncludeInCode(Integer.valueOf(0).equals(segment.getIncludeInCode()) ? 0 : 1);
        segment.setResetEnabled(Integer.valueOf(1).equals(segment.getResetEnabled()) ? 1 : 0);
        segment.setResetPolicy(StringUtils.upperCase(StringUtils.defaultIfBlank(segment.getResetPolicy(), "NONE")));
        segment.setRadixType(StringUtils.upperCase(StringUtils.defaultIfBlank(segment.getRadixType(), "DECIMAL")));
        segment.setStartValue(segment.getStartValue() == null ? 1L : segment.getStartValue());
        segment.setExcludeAmbiguous(Integer.valueOf(1).equals(segment.getExcludeAmbiguous()) ? 1 : 0);
        if (StringUtils.isBlank(segment.getSegmentKey()) || !segment.getSegmentKey().matches("[A-Za-z0-9_-]{1,32}")) {
            throw new BusinessException("分段键只能包含字母、数字、下划线或短横线，且长度不超过32");
        }
        if (!SEGMENT_TYPES.contains(segment.getSegmentType())) {
            throw new BusinessException("不支持的编码分段类型: " + segment.getSegmentType());
        }
    }

    private void validateSequence(CodeRuleSegmentDTO segment) {
        if (segment.getSegmentLength() == null || segment.getSegmentLength() < 1 || segment.getSegmentLength() > 32) {
            throw new BusinessException("流水号长度必须在1到32之间");
        }
        if (!RADIX_TYPES.contains(segment.getRadixType())) {
            throw new BusinessException("不支持的流水号进制: " + segment.getRadixType());
        }
        if (segment.getStartValue() == null || segment.getStartValue() < 0) {
            throw new BusinessException("流水号起始值不能小于0");
        }
        segment.setPadEnabled(1);
        segment.setPadDirection("LEFT");
        segment.setPadChar(radixCodec.alphabet(segment.getRadixType(),
                Integer.valueOf(1).equals(segment.getExcludeAmbiguous())).substring(0, 1));
        if (!Integer.valueOf(1).equals(segment.getResetEnabled())) {
            segment.setResetPolicy("NONE");
        }
        if (!RESET_POLICIES.contains(segment.getResetPolicy())) {
            throw new BusinessException("不支持的流水重置周期: " + segment.getResetPolicy());
        }
        radixCodec.encode(segment.getStartValue(), segment.getRadixType(), segment.getSegmentLength(),
                Integer.valueOf(1).equals(segment.getExcludeAmbiguous()));
    }

    private void validateValueSegment(CodeRuleSegmentDTO segment) {
        if ("DATE".equals(segment.getSegmentType()) && !DATE_FORMATS.contains(segment.getSegmentValue())) {
            throw new BusinessException("不支持的日期格式: " + segment.getSegmentValue());
        }
        if ("FIXED".equals(segment.getSegmentType()) && StringUtils.isBlank(segment.getSegmentValue())) {
            throw new BusinessException("固定值分段不能为空");
        }
        if (("VARIABLE".equals(segment.getSegmentType()) || "SYS_VAR".equals(segment.getSegmentType()))
                && StringUtils.isBlank(segment.getSegmentValue())) {
            throw new BusinessException("变量分段必须选择变量名");
        }
        if ("VARIABLE".equals(segment.getSegmentType())) {
            if (!VARIABLE_SOURCES.contains(segment.getVariableSource())) {
                throw new BusinessException("不支持的业务变量来源: " + segment.getVariableSource());
            }
            if (!VARIABLE_NAME_PATTERN.matcher(segment.getSegmentValue()).matches()) {
                throw new BusinessException("业务变量名必须以字母或下划线开头，且只能包含字母、数字和下划线");
            }
        }
        if ("SYS_VAR".equals(segment.getSegmentType()) && !SYSTEM_VARIABLES.contains(segment.getSegmentValue())) {
            throw new BusinessException("不支持的系统变量: " + segment.getSegmentValue());
        }
        if (segment.getSegmentLength() != null && (segment.getSegmentLength() < 1 || segment.getSegmentLength() > 96)) {
            throw new BusinessException("分段长度必须在1到96之间");
        }
        if (Integer.valueOf(1).equals(segment.getPadEnabled())) {
            String pad = StringUtils.defaultIfBlank(segment.getPadChar(), "0");
            if (pad.codePointCount(0, pad.length()) != 1) {
                throw new BusinessException("补位字符只能包含一个字符");
            }
            segment.setPadChar(pad);
        }
    }

    private List<CodeRuleSegmentDTO> orderedSegments(CodeRuleDefinition definition) {
        if (definition == null || definition.getSegments() == null) {
            return List.of();
        }
        return definition.getSegments().stream()
                .sorted(Comparator.nullsLast(
                        Comparator.comparing(CodeRuleSegmentDTO::getSegmentOrder,
                                Comparator.nullsLast(Integer::compareTo))))
                .toList();
    }

    private int declaredLength(CodeRuleSegmentDTO segment) {
        if (segment.getSegmentLength() != null) {
            return segment.getSegmentLength();
        }
        if ("DATE".equals(segment.getSegmentType()) || "FIXED".equals(segment.getSegmentType())) {
            return StringUtils.length(segment.getSegmentValue());
        }
        return 0;
    }

    private String sampleValue(CodeRuleSegmentDTO segment) {
        int length = segment.getSegmentLength() == null ? Math.min(segment.getSegmentValue().length(), 8)
                : segment.getSegmentLength();
        return "X".repeat(Math.max(1, length));
    }

    private String toCompatibilityTemplate(List<CodeRuleSegmentDTO> segments) {
        StringBuilder template = new StringBuilder();
        for (CodeRuleSegmentDTO segment : segments) {
            if (!Integer.valueOf(1).equals(segment.getIncludeInCode())) {
                continue;
            }
            template.append(switch (segment.getSegmentType()) {
                case "FIXED" -> StringUtils.defaultString(segment.getSegmentValue());
                case "DATE" -> "${" + segment.getSegmentValue() + "}";
                case "SEQ" -> "${seq:" + segment.getSegmentLength() + "}";
                case "VARIABLE" -> "${field:" + segment.getSegmentValue() + "}";
                case "SYS_VAR" -> "${" + segment.getSegmentValue() + "}";
                default -> "";
            });
        }
        return template.toString();
    }

    private String toFormatExpression(List<CodeRuleSegmentDTO> segments) {
        return segments.stream()
                .filter(segment -> Integer.valueOf(1).equals(segment.getIncludeInCode()))
                .map(segment -> "[" + segment.getSegmentType() + ":"
                        + StringUtils.defaultIfBlank(segment.getSegmentValue(), String.valueOf(segment.getSegmentLength())) + "]")
                .reduce((left, right) -> left + right)
                .orElse("");
    }

    private CodeRulePreviewVO.SegmentPreviewVO segmentPreview(CodeRuleSegmentDTO segment, String value) {
        CodeRulePreviewVO.SegmentPreviewVO item = new CodeRulePreviewVO.SegmentPreviewVO();
        item.setSegmentKey(segment.getSegmentKey());
        item.setSegmentOrder(segment.getSegmentOrder());
        item.setSegmentType(segment.getSegmentType());
        item.setExpression(StringUtils.defaultIfBlank(segment.getSegmentValue(), String.valueOf(segment.getSegmentLength())));
        item.setValue(value);
        item.setIncluded(Integer.valueOf(1).equals(segment.getIncludeInCode()));
        item.setGrouped(Integer.valueOf(1).equals(segment.getGroupEnabled()));
        return item;
    }

    private CodeRulePreviewVO.PreviewIssueVO issue(String token, String message, String suggestion) {
        CodeRulePreviewVO.PreviewIssueVO issue = new CodeRulePreviewVO.PreviewIssueVO();
        issue.setToken(token);
        issue.setMessage(message);
        issue.setSuggestion(suggestion);
        return issue;
    }

    private Map<String, Object> safeMap(Map<String, Object> source) {
        return source == null ? Map.of() : source;
    }

    private Map<String, Object> safeBusinessFields(Map<String, Object> source) {
        Map<String, Object> fields = safeMap(source);
        if (fields.size() > MAX_BUSINESS_FIELD_COUNT) {
            throw new BusinessException("业务上下文字段不能超过" + MAX_BUSINESS_FIELD_COUNT + "个");
        }
        return fields;
    }

    private record RenderResult(CodeRulePreviewVO preview,
                                String code,
                                Long sequence,
                                String groupKey,
                                String period) {
    }

    private record LegacyWidthCacheKey(Long tenantId,
                                       Long ruleId,
                                       String segmentKey,
                                       String period,
                                       String radixType,
                                       int configuredLength,
                                       boolean excludeAmbiguous) {
    }
}
