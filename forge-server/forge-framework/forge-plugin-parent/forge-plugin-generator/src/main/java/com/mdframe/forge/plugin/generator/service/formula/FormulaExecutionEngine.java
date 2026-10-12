package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Formula execution engine - orchestrates formula evaluation for a business object instance.
 * <p>
 * Execution flow:
 * <ol>
 *   <li>Pre-validate formulas via {@link FormulaDependencyAnalyzer} (cycle/depth check)</li>
 *   <li>Build execution plan from topological order</li>
 *   <li>Execute each formula in order, resolving dependencies from already-computed values</li>
 *   <li>Handle errors via {@link FormulaErrorHandler} (degrade or fail-fast)</li>
 *   <li>Return {@link ExecutionResult} with computed values and error details</li>
 * </ol>
 */
@Component
public class FormulaExecutionEngine {

    private final ExpressionExecutor executor;
    private final FormulaDependencyAnalyzer dependencyAnalyzer;
    private final FormulaErrorHandler errorHandler;
    private final AggregateEngine aggregateEngine;
    private final FormulaLookupResolver lookupResolver;
    private final FormulaFunctionMarketService functionMarketService;

    /**
     * Preferred constructor - requires an AggregateDataProvider for AGGREGATE formula support.
     */
    public FormulaExecutionEngine(AggregateDataProvider dataProvider) {
        this(dataProvider, FormulaLookupResolver.unsupported());
    }

    public FormulaExecutionEngine(AggregateDataProvider dataProvider,
                                  FormulaLookupResolver lookupResolver) {
        this(dataProvider, lookupResolver, FormulaFunctionRegistry.builtin());
    }

    public FormulaExecutionEngine(AggregateDataProvider dataProvider,
                                  FormulaLookupResolver lookupResolver,
                                  FormulaFunctionRegistry functionRegistry) {
        this(dataProvider, lookupResolver, functionRegistry, null);
    }

    @Autowired
    public FormulaExecutionEngine(AggregateDataProvider dataProvider,
                                  FormulaLookupResolver lookupResolver,
                                  FormulaFunctionRegistry functionRegistry,
                                  FormulaFunctionMarketService functionMarketService) {
        this(new ExpressionExecutor(new AviatorAdapter(), functionRegistry), new FormulaDependencyAnalyzer(),
             new FormulaErrorHandler(),
             new AggregateEngine(new AggregateFunctionExecutor(),
                 new ExpressionExecutor(new AviatorAdapter(), functionRegistry), dataProvider),
             lookupResolver,
             functionMarketService);
    }

    public FormulaExecutionEngine(ExpressionExecutor executor,
                                   FormulaDependencyAnalyzer dependencyAnalyzer,
                                   FormulaErrorHandler errorHandler,
                                   AggregateEngine aggregateEngine) {
        this(executor, dependencyAnalyzer, errorHandler, aggregateEngine,
            FormulaLookupResolver.unsupported());
    }

    public FormulaExecutionEngine(ExpressionExecutor executor,
                                   FormulaDependencyAnalyzer dependencyAnalyzer,
                                   FormulaErrorHandler errorHandler,
                                   AggregateEngine aggregateEngine,
                                   FormulaLookupResolver lookupResolver) {
        this(executor, dependencyAnalyzer, errorHandler, aggregateEngine, lookupResolver, null);
    }

    public FormulaExecutionEngine(ExpressionExecutor executor,
                                   FormulaDependencyAnalyzer dependencyAnalyzer,
                                   FormulaErrorHandler errorHandler,
                                   AggregateEngine aggregateEngine,
                                   FormulaLookupResolver lookupResolver,
                                   FormulaFunctionMarketService functionMarketService) {
        this.executor = Objects.requireNonNull(executor);
        this.dependencyAnalyzer = Objects.requireNonNull(dependencyAnalyzer);
        this.errorHandler = Objects.requireNonNull(errorHandler);
        this.aggregateEngine = Objects.requireNonNull(aggregateEngine);
        this.lookupResolver = Objects.requireNonNull(lookupResolver);
        this.functionMarketService = functionMarketService;
    }

    @Deprecated
    public FormulaExecutionEngine() {
        this(new ExpressionExecutor(), new FormulaDependencyAnalyzer(),
             new FormulaErrorHandler(), new AggregateEngine());
    }

    public ExecutionResult execute(Map<String, FormulaConfig> formulaMap,
                                    Map<String, Object> originalValues) {
        return execute(formulaMap, originalValues, FormulaTraceOptions.disabled());
    }

    public ExecutionResult execute(Map<String, FormulaConfig> formulaMap,
                                    Map<String, Object> originalValues,
                                    FormulaTraceOptions traceOptions) {
        Objects.requireNonNull(formulaMap);
        Objects.requireNonNull(originalValues);
        syncFunctionDefinitions();

        FormulaTraceOptions effectiveTraceOptions =
            traceOptions == null ? FormulaTraceOptions.disabled() : traceOptions;
        String traceId = effectiveTraceOptions.isEnabled() ? newTraceId() : null;
        FormulaExecutionTrace.Builder traceBuilder = effectiveTraceOptions.isEnabled()
            ? FormulaExecutionTrace.builder().traceId(traceId)
            : null;

        long startTime = System.currentTimeMillis();

        if (formulaMap.isEmpty()) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            ExecutionResult.Builder result = ExecutionResult.builder().success(true).elapsedMs(elapsedMs);
            attachTrace(result, traceBuilder, Collections.emptyList(), elapsedMs);
            return result.build();
        }

        DependencyAnalysisResult depResult = dependencyAnalyzer.analyze(formulaMap);
        if (depResult.hasCycle()) {
            ExecutionResult.Builder result = ExecutionResult.builder().success(false);
            for (String err : depResult.getErrors()) {
                result.putError("DAG", err);
                if (traceBuilder != null) traceBuilder.addError(err);
            }
            long elapsedMs = System.currentTimeMillis() - startTime;
            result.elapsedMs(elapsedMs);
            attachTrace(result, traceBuilder, depResult.getTopologicalOrder(), elapsedMs);
            return result.build();
        }

        Map<String, Object> context = new LinkedHashMap<>(originalValues);
        List<FormulaErrorHandler.HandleResult> handleResults = new ArrayList<>();
        ExecutionResult.Builder resultBuilder = ExecutionResult.builder();
        boolean hasErrors = false;

        for (String fieldName : depResult.getTopologicalOrder()) {
            FormulaConfig config = formulaMap.get(fieldName);
            if (config == null) continue;

            long stepStartTime = System.currentTimeMillis();
            Map<String, Object> inputSnapshot = snapshotInput(context, effectiveTraceOptions);
            StepOutcome outcome;

            if (config.isAggregate()) {
                outcome = executeAggregate(fieldName, config, context, resultBuilder, handleResults, traceId);
                addTraceStep(traceBuilder, fieldName, config, inputSnapshot, outcome,
                    System.currentTimeMillis() - stepStartTime, effectiveTraceOptions);
                if (!outcome.success()) hasErrors = true;
                continue;
            }

            if (config.isConditional()) {
                outcome = executeConditional(fieldName, config, context, resultBuilder, handleResults, traceId);
                addTraceStep(traceBuilder, fieldName, config, inputSnapshot, outcome,
                    System.currentTimeMillis() - stepStartTime, effectiveTraceOptions);
                if (!outcome.success()) hasErrors = true;
                continue;
            }

            if (config.isLookup()) {
                outcome = executeLookup(fieldName, config, context, resultBuilder, handleResults, traceId);
                addTraceStep(traceBuilder, fieldName, config, inputSnapshot, outcome,
                    System.currentTimeMillis() - stepStartTime, effectiveTraceOptions);
                if (!outcome.success()) hasErrors = true;
                continue;
            }

            if (!config.isCalc()) continue;

            outcome = executeCalc(fieldName, config, context, resultBuilder, handleResults, traceId);
            addTraceStep(traceBuilder, fieldName, config, inputSnapshot, outcome,
                System.currentTimeMillis() - stepStartTime, effectiveTraceOptions);
            if (!outcome.success()) hasErrors = true;
        }

        for (Map.Entry<String, Object> entry : originalValues.entrySet()) {
            context.putIfAbsent(entry.getKey(), entry.getValue());
        }

        boolean success = !hasErrors;
        resultBuilder.success(success);
        long elapsedMs = System.currentTimeMillis() - startTime;
        resultBuilder.elapsedMs(elapsedMs);
        attachTrace(resultBuilder, traceBuilder, depResult.getTopologicalOrder(), elapsedMs);

        if (!handleResults.isEmpty()) errorHandler.summarize(handleResults);

        return resultBuilder.build();
    }

    private void syncFunctionDefinitions() {
        if (functionMarketService == null) {
            return;
        }
        functionMarketService.syncInstalledDefinitions();
    }

    private StepOutcome executeCalc(String fieldName, FormulaConfig config,
                                     Map<String, Object> context,
                                     ExecutionResult.Builder resultBuilder,
                                     List<FormulaErrorHandler.HandleResult> handleResults,
                                     String traceId) {
        String expression = config.getExpression();
        try {
            Object value = executor.execute(expression, context);
            context.put(fieldName, value);
            resultBuilder.putResult(fieldName, value);
            return StepOutcome.success(value);
        } catch (FormulaExecutionException e) {
            Object fallback = context.getOrDefault(fieldName, null);
            FormulaErrorHandler.HandleResult hr =
                errorHandler.handleError(fieldName, expression, e, fallback, traceId);
            handleResults.add(hr);
            context.put(fieldName, hr.fallbackValue);
            resultBuilder.putError(fieldName, hr.errorMessage);
            return StepOutcome.failure(hr.fallbackValue, hr.errorMessage);
        } catch (Exception e) {
            FormulaErrorHandler.HandleResult hr =
                errorHandler.handleError(fieldName, expression, e, null, traceId);
            handleResults.add(hr);
            context.put(fieldName, null);
            resultBuilder.putError(fieldName, hr.errorMessage);
            return StepOutcome.failure(null, hr.errorMessage);
        }
    }

    private StepOutcome executeLookup(String fieldName, FormulaConfig config,
                                      Map<String, Object> context,
                                      ExecutionResult.Builder resultBuilder,
                                      List<FormulaErrorHandler.HandleResult> handleResults,
                                      String traceId) {
        LookupConfig lookupConfig = config.getLookup();
        if (lookupConfig == null) {
            String errorMessage = "LOOKUP formula missing lookup config";
            resultBuilder.putError(fieldName, errorMessage);
            return StepOutcome.failure(null, errorMessage);
        }
        try {
            LookupResolveResult lookupResult = lookupResolver.resolve(lookupConfig, context);
            if (lookupResult.isSuccess()) {
                context.put(fieldName, lookupResult.getValue());
                resultBuilder.putResult(fieldName, lookupResult.getValue());
                Map<String, Object> metadata = new LinkedHashMap<>(lookupResult.getMetadata());
                metadata.put("lookupMatched", lookupResult.isMatched());
                return StepOutcome.success(lookupResult.getValue(), metadata);
            }
            FormulaErrorHandler.HandleResult hr =
                errorHandler.handleError(fieldName, lookupConfig.toString(),
                    new FormulaExecutionException(lookupResult.getErrorMessage()),
                    lookupConfig.getNotFoundValue(), traceId);
            handleResults.add(hr);
            context.put(fieldName, hr.fallbackValue);
            resultBuilder.putError(fieldName, hr.errorMessage);
            Map<String, Object> metadata = new LinkedHashMap<>(lookupResult.getMetadata());
            // 降级值不仅供后续公式使用，也必须保留在对外结果中；错误标记仍保持失败。
            resultBuilder.putResult(fieldName, hr.fallbackValue);
            metadata.put("lookupMatched", false);
            return StepOutcome.failure(hr.fallbackValue, hr.errorMessage, metadata);
        } catch (Exception e) {
            FormulaErrorHandler.HandleResult hr =
                errorHandler.handleError(fieldName, lookupConfig.toString(), e,
                    lookupConfig.getNotFoundValue(), traceId);
            handleResults.add(hr);
            context.put(fieldName, hr.fallbackValue);
            resultBuilder.putError(fieldName, hr.errorMessage);
            resultBuilder.putResult(fieldName, hr.fallbackValue);
            return StepOutcome.failure(hr.fallbackValue, hr.errorMessage);
        }
    }

    private StepOutcome executeConditional(String fieldName, FormulaConfig config,
                                     Map<String, Object> context,
                                     ExecutionResult.Builder resultBuilder,
                                     List<FormulaErrorHandler.HandleResult> handleResults,
                                     String traceId) {
        ConditionConfig cc = config.getCondition();
        if (cc == null) {
            String errorMessage = "CONDITIONAL formula missing condition config";
            resultBuilder.putError(fieldName, errorMessage);
            return StepOutcome.failure(null, errorMessage);
        }
        try {
            Object flag = executor.execute(cc.getExpression(), context);
            boolean matched = isTruthy(flag);
            Object value = matched ? cc.getTrueValue() : cc.getFalseValue();
            context.put(fieldName, value);
            resultBuilder.putResult(fieldName, value);
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("conditionResult", flag);
            metadata.put("conditionMatched", matched);
            metadata.put("trueValue", cc.getTrueValue());
            metadata.put("falseValue", cc.getFalseValue());
            return StepOutcome.success(value, metadata);
        } catch (FormulaExecutionException e) {
            Object fallback = context.getOrDefault(fieldName, cc.getFalseValue());
            FormulaErrorHandler.HandleResult hr =
                errorHandler.handleError(fieldName, cc.getExpression(), e, fallback, traceId);
            handleResults.add(hr);
            context.put(fieldName, hr.fallbackValue);
            resultBuilder.putError(fieldName, hr.errorMessage);
            return StepOutcome.failure(hr.fallbackValue, hr.errorMessage);
        } catch (Exception e) {
            FormulaErrorHandler.HandleResult hr =
                errorHandler.handleError(fieldName, cc.getExpression(), e, cc.getFalseValue(), traceId);
            handleResults.add(hr);
            context.put(fieldName, hr.fallbackValue);
            resultBuilder.putError(fieldName, hr.errorMessage);
            return StepOutcome.failure(hr.fallbackValue, hr.errorMessage);
        }
    }

    private StepOutcome executeAggregate(String fieldName, FormulaConfig config,
                                   Map<String, Object> context,
                                   ExecutionResult.Builder resultBuilder,
                                   List<FormulaErrorHandler.HandleResult> handleResults,
                                   String traceId) {
        AggregateConfig aggConfig = config.getAggregate();
        if (aggConfig == null) {
            String errorMessage = "AGGREGATE formula missing aggregate config";
            resultBuilder.putError(fieldName, errorMessage);
            return StepOutcome.failure(null, errorMessage);
        }
        try {
            AggregateResult aggResult = aggregateEngine.execute(aggConfig, context);
            if (aggResult.isSuccess()) {
                context.put(fieldName, aggResult.getValue());
                resultBuilder.putResult(fieldName, aggResult.getValue());
                return StepOutcome.success(aggResult.getValue());
            } else {
                for (String err : aggResult.getErrors()) {
                    resultBuilder.putError(fieldName, err);
                }
                return StepOutcome.failure(null, String.join("; ", aggResult.getErrors()));
            }
        } catch (Exception e) {
            FormulaErrorHandler.HandleResult hr =
                errorHandler.handleError(fieldName, aggConfig.toString(), e, null, traceId);
            handleResults.add(hr);
            context.put(fieldName, null);
            resultBuilder.putError(fieldName, hr.errorMessage);
            return StepOutcome.failure(null, hr.errorMessage);
        }
    }

    private void attachTrace(ExecutionResult.Builder resultBuilder,
                             FormulaExecutionTrace.Builder traceBuilder,
                             List<String> executionPlan,
                             long elapsedMs) {
        if (traceBuilder == null) {
            return;
        }
        resultBuilder.trace(traceBuilder.executionPlan(executionPlan).elapsedMs(elapsedMs).build());
    }

    private void addTraceStep(FormulaExecutionTrace.Builder traceBuilder,
                              String fieldName,
                              FormulaConfig config,
                              Map<String, Object> inputSnapshot,
                              StepOutcome outcome,
                              long elapsedMs,
                              FormulaTraceOptions traceOptions) {
        if (traceBuilder == null) {
            return;
        }
        FormulaExecutionStep.Builder step = FormulaExecutionStep.builder()
            .fieldCode(fieldName)
            .formulaType(config.getType().name())
            .expression(expressionSummary(config))
            .input(inputSnapshot)
            .output(traceOptions.isIncludeOutputValue() ? outcome.output() : null)
            .elapsedMs(elapsedMs)
            .success(outcome.success())
            .errorMessage(outcome.errorMessage())
            .putMetadata("formulaMode", config.getMode().name());
        for (Map.Entry<String, Object> entry : outcome.metadata().entrySet()) {
            step.putMetadata(entry.getKey(), entry.getValue());
        }
        FormulaExecutionStep traceStep = step.build();
        traceBuilder.addStep(traceStep);
        if (!outcome.success()) {
            traceBuilder.addError(fieldName + ": " + outcome.errorMessage());
        }
    }

    private Map<String, Object> snapshotInput(Map<String, Object> context,
                                              FormulaTraceOptions traceOptions) {
        if (!traceOptions.isIncludeInputSnapshot()) {
            return Collections.emptyMap();
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (!entry.getKey().startsWith("__")) {
                snapshot.put(entry.getKey(), entry.getValue());
            }
        }
        return snapshot;
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
        if (config.isLookup() && config.getLookup() != null) {
            return config.getLookup().toString();
        }
        if (config.hasCrossObject()) {
            return config.getCrossObject().getPath();
        }
        return null;
    }

    private String newTraceId() {
        return "FML-" + UUID.randomUUID().toString().replace("-", "");
    }

    private boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean b) return b;
        if (val instanceof Number n) return n.doubleValue() != 0;
        if (val instanceof String s) return !s.isBlank() && !"false".equalsIgnoreCase(s);
        return true;
    }

    private record StepOutcome(boolean success, Object output, String errorMessage, Map<String, Object> metadata) {
        static StepOutcome success(Object output) {
            return new StepOutcome(true, output, null, Collections.emptyMap());
        }

        static StepOutcome success(Object output, Map<String, Object> metadata) {
            return new StepOutcome(true, output, null,
                metadata == null ? Collections.emptyMap() : new LinkedHashMap<>(metadata));
        }

        static StepOutcome failure(Object output, String errorMessage) {
            return new StepOutcome(false, output, errorMessage, Collections.emptyMap());
        }

        static StepOutcome failure(Object output, String errorMessage, Map<String, Object> metadata) {
            return new StepOutcome(false, output, errorMessage,
                metadata == null ? Collections.emptyMap() : new LinkedHashMap<>(metadata));
        }
    }
}
