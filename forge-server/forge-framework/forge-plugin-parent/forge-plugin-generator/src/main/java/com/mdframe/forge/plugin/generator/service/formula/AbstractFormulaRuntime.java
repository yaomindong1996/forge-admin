package com.mdframe.forge.plugin.generator.service.formula;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.formula.*;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaExecutionLogResponse;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Abstract base for formula runtime execution (STORED / VIRTUAL).
 * <p>
 * Template method pattern: subclasses override {@link #extractFormulas(LowcodeModelSchema)}
 * to filter by mode, while the execution loop is shared.
 */
public abstract class AbstractFormulaRuntime {

    private static final Logger log = LoggerFactory.getLogger(AbstractFormulaRuntime.class);

    protected final FormulaExecutionEngine executionEngine;
    protected final ObjectMapper objectMapper;
    protected final FormulaExecutionLogService executionLogService;
    protected final FormulaRuntimeProperties runtimeProperties;
    private final String modeLabel;

    protected AbstractFormulaRuntime(FormulaExecutionEngine executionEngine,
                                      ObjectMapper objectMapper,
                                      String modeLabel) {
        this(executionEngine, objectMapper, modeLabel, null);
    }

    protected AbstractFormulaRuntime(FormulaExecutionEngine executionEngine,
                                      ObjectMapper objectMapper,
                                      String modeLabel,
                                      FormulaExecutionLogService executionLogService) {
        this(executionEngine, objectMapper, modeLabel, executionLogService, new FormulaRuntimeProperties());
    }

    protected AbstractFormulaRuntime(FormulaExecutionEngine executionEngine,
                                      ObjectMapper objectMapper,
                                      String modeLabel,
                                      FormulaExecutionLogService executionLogService,
                                      FormulaRuntimeProperties runtimeProperties) {
        this.executionEngine = Objects.requireNonNull(executionEngine);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.modeLabel = Objects.requireNonNull(modeLabel);
        this.executionLogService = executionLogService;
        this.runtimeProperties = runtimeProperties == null ? new FormulaRuntimeProperties() : runtimeProperties;
    }

    protected abstract Map<String, FormulaConfig> extractFormulas(LowcodeModelSchema modelSchema);

    public List<Map<String, Object>> calculate(List<Map<String, Object>> records,
                                                LowcodeModelSchema modelSchema,
                                                FormulaRuntimeContext context) {
        return calculate(records, modelSchema, context, FormulaTraceOptions.disabled());
    }

    public List<Map<String, Object>> calculate(List<Map<String, Object>> records,
                                                LowcodeModelSchema modelSchema,
                                                FormulaRuntimeContext context,
                                                FormulaTraceOptions traceOptions) {
        if (records == null || records.isEmpty()) return records != null ? records : List.of();
        if (modelSchema == null || modelSchema.getFields() == null) return records;

        Map<String, FormulaConfig> formulas = extractFormulas(modelSchema);
        if (formulas.isEmpty()) return records;

        Map<String, Object> execCtx = buildExecutionContext(context);
        FormulaTraceOptions effectiveTraceOptions = runtimeTraceOptions(traceOptions);

        for (Map<String, Object> record : records) {
            Map<String, Object> rowContext = new LinkedHashMap<>(record);
            if (context != null) {
                rowContext.put("tenantId", context.getTenantId());
                rowContext.put("suiteCode", context.getSuiteCode());
                rowContext.put("sourceObjectCode", context.getSourceObjectCode());
            }
            rowContext.putAll(execCtx);

            ExecutionResult execResult = executionEngine.execute(formulas, rowContext, effectiveTraceOptions);

            for (Map.Entry<String, Object> entry : execResult.getResults().entrySet()) {
                record.put(entry.getKey(), entry.getValue());
                log.debug("{} formula [{}] = {}", modeLabel, entry.getKey(), entry.getValue());
            }

            if (!execResult.getErrors().isEmpty()) {
                for (Map.Entry<String, List<String>> e : execResult.getErrors().entrySet()) {
                    for (String msg : e.getValue()) {
                        log.error("{} formula error: traceId={}, field={}, message={}",
                            modeLabel, traceId(execResult), e.getKey(), msg);
                    }
                }
            }

            recordExecutionLogs(record, context, formulas, execResult, effectiveTraceOptions);
        }

        return records;
    }

    protected Map<String, FormulaConfig> extractByMode(LowcodeModelSchema modelSchema,
                                                         FormulaMode targetMode) {
        Map<String, FormulaConfig> result = new LinkedHashMap<>();
        if (modelSchema == null || modelSchema.getFields() == null) return result;

        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            Map<String, Object> fc = field.getFormulaConfig();
            if (fc == null) continue;

            FormulaConfig config = parseFormulaConfig(fc);
            if (config != null && config.getMode() == targetMode) {
                result.put(field.getField(), config);
            }
        }
        return result;
    }

    public FormulaConfig parseFormulaConfig(Map<String, Object> fc) {
        try {
            Map<String, Object> map = objectMapper.convertValue(fc,
                new TypeReference<Map<String, Object>>() {});

            FormulaType type = FormulaType.valueOf(text(map.get("type"), "CALC"));
            FormulaMode mode = FormulaMode.valueOf(text(map.get("mode"), "STORED"));
            String expression = text(map.get("expression"), null);
            List<String> dependsOn = stringList(map.get("dependsOn"));

            FormulaConfig.Builder builder = FormulaConfig.builder()
                .type(type)
                .mode(mode)
                .expression(expression)
                .dependsOn(dependsOn)
                .crossObject(parseCrossObjectConfig(map.get("crossObject")))
                .rule(objectMap(map.get("rule")))
                .functionRefs(stringList(map.get("functionRefs")));

            if (type == FormulaType.AGGREGATE) {
                AggregateConfig aggregate = parseAggregateConfig(map.get("aggregate"));
                if (aggregate != null) builder.aggregate(aggregate);
            }
            if (type == FormulaType.CONDITIONAL) {
                ConditionConfig cc = parseConditionConfig(map.get("condition"));
                if (cc != null) builder.condition(cc);
            }
            if (type == FormulaType.LOOKUP) {
                LookupConfig lookup = parseLookupConfig(map.get("lookup"));
                if (lookup != null) builder.lookup(lookup);
            }

            return builder.build();
        } catch (Exception e) {
            log.warn("Failed to parse formula config: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private ConditionConfig parseConditionConfig(Object raw) {
        if (raw == null) return null;
        try {
            Map<String, Object> map = objectMapper.convertValue(raw,
                new TypeReference<Map<String, Object>>() {});
            String expression = text(map.get("expression"), null);
            if (expression == null || expression.isBlank()) return null;
            return new ConditionConfig(expression, map.get("trueValue"), map.get("falseValue"));
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private AggregateConfig parseAggregateConfig(Object raw) {
        if (raw == null) return null;
        try {
            Map<String, Object> map = objectMapper.convertValue(raw,
                new TypeReference<Map<String, Object>>() {});
            AggregateFunction function = AggregateFunction.valueOf(
                text(map.get("function"), "SUM"));
            String relationCode = text(map.get("relationCode"), null);
            String targetField = text(map.get("targetField"), null);
            String filter = text(map.get("filter"), null);
            if (relationCode == null || targetField == null) return null;
            return new AggregateConfig(function, relationCode, targetField, filter);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private LookupConfig parseLookupConfig(Object raw) {
        if (raw == null) return null;
        try {
            Map<String, Object> map = objectMapper.convertValue(raw,
                new TypeReference<Map<String, Object>>() {});
            return new LookupConfig(
                text(map.get("relationCode"), null),
                text(map.get("targetObjectCode"), null),
                text(map.get("sourceField"), null),
                text(map.get("targetField"), null),
                text(map.get("returnField"), null),
                map.get("notFoundValue"));
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private CrossObjectConfig parseCrossObjectConfig(Object raw) {
        if (raw == null) return null;
        try {
            Map<String, Object> map = objectMapper.convertValue(raw,
                new TypeReference<Map<String, Object>>() {});
            CrossObjectRecomputeMode recomputeMode = parseEnum(
                CrossObjectRecomputeMode.class,
                text(map.get("recomputeMode"), null),
                CrossObjectRecomputeMode.ASYNC);
            return new CrossObjectConfig(
                text(map.get("path"), null),
                text(map.get("relationCode"), null),
                text(map.get("targetObjectCode"), null),
                text(map.get("returnField"), null),
                recomputeMode);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(Object val) {
        if (val == null) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.convertValue(val, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, E fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (Exception e) {
            return fallback;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object val) {
        if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return Collections.emptyList();
    }

    private String text(Object val, String fallback) {
        return val != null ? val.toString() : fallback;
    }

    protected final Map<String, Object> buildExecutionContext(FormulaRuntimeContext context) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        if (context != null) {
            ctx.put("__formulaRuntimeContext__", context);
            ctx.put("tenantId", context.getTenantId());
            ctx.put("suiteCode", context.getSuiteCode());
            ctx.put("sourceObjectCode", context.getSourceObjectCode());
        }
        return ctx;
    }

    private FormulaTraceOptions runtimeTraceOptions(FormulaTraceOptions traceOptions) {
        if (traceOptions != null && traceOptions.isDebugMode()) {
            return traceOptions;
        }
        if (traceOptions != null && traceOptions.isEnabled()) {
            return traceOptions;
        }
        return FormulaTraceOptions.builder()
                .enabled(runtimeProperties.isExecutionLogEnabled()
                        && (runtimeProperties.isFailureLogEnabled() || runtimeProperties.isSuccessLogEnabled()))
                .includeInputSnapshot(false)
                .includeOutputValue(false)
                .build();
    }

    private void recordExecutionLogs(Map<String, Object> record,
                                     FormulaRuntimeContext context,
                                     Map<String, FormulaConfig> formulas,
                                     ExecutionResult execResult,
                                     FormulaTraceOptions traceOptions) {
        if (executionLogService == null || !runtimeProperties.isExecutionLogEnabled()) {
            return;
        }

        if (traceOptions.isDebugMode() && execResult.hasTrace()) {
            if (!runtimeProperties.isDebugLogEnabled()) {
                return;
            }
            for (FormulaExecutionStep step : execResult.getTrace().getSteps()) {
                recordStepLog(record, context, formulas, execResult.getTrace().getTraceId(), step);
            }
            return;
        }

        if (execResult.getErrors().isEmpty()) {
            if (!runtimeProperties.isSuccessLogEnabled() || !execResult.hasTrace()) {
                return;
            }
            for (FormulaExecutionStep step : execResult.getTrace().getSteps()) {
                recordStepLog(record, context, formulas, execResult.getTrace().getTraceId(), step);
            }
            return;
        }

        if (!runtimeProperties.isFailureLogEnabled()) {
            return;
        }

        Set<String> loggedFields = new HashSet<>();
        if (execResult.hasTrace()) {
            for (FormulaExecutionStep step : execResult.getTrace().getSteps()) {
                if (!step.isSuccess()) {
                    recordStepLog(record, context, formulas, execResult.getTrace().getTraceId(), step);
                    loggedFields.add(step.getFieldCode());
                }
            }
        }

        for (Map.Entry<String, List<String>> entry : execResult.getErrors().entrySet()) {
            if (loggedFields.contains(entry.getKey())) {
                continue;
            }
            recordErrorLog(record, context, formulas, traceId(execResult), entry.getKey(), entry.getValue());
        }
    }

    private void recordStepLog(Map<String, Object> record,
                               FormulaRuntimeContext context,
                               Map<String, FormulaConfig> formulas,
                               String traceId,
                               FormulaExecutionStep step) {
        FormulaConfig config = formulas.get(step.getFieldCode());
        FormulaExecutionLogResponse logEntry = FormulaExecutionLogResponse.builder()
            .tenantId(context == null ? null : context.getTenantId())
            .traceId(traceId)
            .objectCode(context == null ? null : context.getSourceObjectCode())
            .recordId(recordId(record))
            .fieldCode(step.getFieldCode())
            .formulaType(config == null ? step.getFormulaType() : config.getType().name())
            .formulaMode(config == null ? modeLabel : config.getMode().name())
            .expression(step.getExpression())
            .inputSnapshot(step.getInput().isEmpty() ? null : toJson(step.getInput()))
            .outputValue(step.getOutput() == null ? null : toJson(step.getOutput()))
            .success(step.isSuccess())
            .errorMessage(step.getErrorMessage())
            .elapsedMs(step.getElapsedMs())
            .build();
        safeRecord(logEntry);
    }

    private void recordErrorLog(Map<String, Object> record,
                                FormulaRuntimeContext context,
                                Map<String, FormulaConfig> formulas,
                                String traceId,
                                String fieldCode,
                                List<String> errors) {
        FormulaConfig config = formulas.get(fieldCode);
        FormulaExecutionLogResponse logEntry = FormulaExecutionLogResponse.builder()
            .tenantId(context == null ? null : context.getTenantId())
            .traceId(traceId)
            .objectCode(context == null ? null : context.getSourceObjectCode())
            .recordId(recordId(record))
            .fieldCode(fieldCode)
            .formulaType(config == null ? null : config.getType().name())
            .formulaMode(config == null ? modeLabel : config.getMode().name())
            .expression(config == null ? null : expressionSummary(config))
            .success(false)
            .errorMessage(String.join("; ", errors))
            .build();
        safeRecord(logEntry);
    }

    private void safeRecord(FormulaExecutionLogResponse logEntry) {
        try {
            executionLogService.record(logEntry);
        } catch (Exception e) {
            log.warn("Failed to record {} formula execution log: {}", modeLabel, e.getMessage());
        }
    }

    private String traceId(ExecutionResult execResult) {
        return execResult.hasTrace() ? execResult.getTrace().getTraceId() : null;
    }

    private String recordId(Map<String, Object> record) {
        if (record == null) {
            return null;
        }
        Object id = record.get("id");
        return id == null ? null : id.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String expressionSummary(FormulaConfig config) {
        if (config.getExpression() != null && !config.getExpression().isBlank()) {
            return config.getExpression();
        }
        if (config.isAggregate() && config.getAggregate() != null) {
            return config.getAggregate().toString();
        }
        if (config.isConditional() && config.getCondition() != null) {
            return config.getCondition().getExpression();
        }
        return null;
    }
}
