package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FormulaExecutionEngine")
@Tag("dev")
class FormulaExecutionEngineTest {

    private final FormulaExecutionEngine engine = new FormulaExecutionEngine();

    private FormulaConfig calc(String expr, List<String> deps) {
        return FormulaConfig.builder()
            .type(FormulaType.CALC)
            .mode(FormulaMode.STORED)
            .expression(expr)
            .dependsOn(deps)
            .build();
    }

    private Map<String, FormulaConfig> fm(Object... kvs) {
        Map<String, FormulaConfig> m = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2)
            m.put((String) kvs[i], (FormulaConfig) kvs[i + 1]);
        return m;
    }

    @Nested
    @DisplayName("basic execution")
    class BasicExecution {
        @Test
        @DisplayName("empty formula map succeeds")
        void empty() {
            ExecutionResult r = engine.execute(Collections.emptyMap(), Map.of());
            assertTrue(r.isSuccess());
            assertEquals(0, r.getSuccessCount());
        }

        @Test
        @DisplayName("single formula with raw field values")
        void singleFormula() {
            Map<String, FormulaConfig> formulas = fm("total",
                calc("price * quantity", List.of("price", "quantity")));

            ExecutionResult r = engine.execute(formulas,
                Map.of("price", 100L, "quantity", 3L));

            assertTrue(r.isSuccess(), "Errors: " + r.getErrors());
            assertEquals(300L, r.getResult("total"));
        }

        @Test
        @DisplayName("chain: C then B then A in topological order")
        void chainExecution() {
            Map<String, FormulaConfig> formulas = fm(
                "A", calc("B + 1", List.of("B")),
                "B", calc("C * 2", List.of("C")),
                "C", calc("42", Collections.emptyList())
            );

            ExecutionResult r = engine.execute(formulas, Map.of());

            assertTrue(r.isSuccess(), "Errors: " + r.getErrors());
            assertEquals(42L, r.getResult("C"));
            assertEquals(84L, r.getResult("B"));
            assertEquals(85L, r.getResult("A"));

            // Verify topological order: C before B before A
            List<String> order = r.getExecutedFields();
            assertTrue(order.indexOf("C") < order.indexOf("B"));
            assertTrue(order.indexOf("B") < order.indexOf("A"));
        }

        @Test
        @DisplayName("tree: A=B+C with B and C independent")
        void treeExecution() {
            Map<String, FormulaConfig> formulas = fm(
                "A", calc("B + C", List.of("B", "C")),
                "B", calc("10", Collections.emptyList()),
                "C", calc("20", Collections.emptyList())
            );

            ExecutionResult r = engine.execute(formulas, Map.of());

            assertTrue(r.isSuccess(), "Errors: " + r.getErrors());
            assertEquals(10L, r.getResult("B"));
            assertEquals(20L, r.getResult("C"));
            assertEquals(30L, r.getResult("A"));
        }

        @Test
        @DisplayName("mixed: raw values + computed formulas")
        void mixedRawAndComputed() {
            Map<String, FormulaConfig> formulas = fm(
                "total", calc("price * quantity", List.of("price", "quantity")),
                "tax", calc("total * taxRate", List.of("total", "taxRate"))
            );

            ExecutionResult r = engine.execute(formulas,
                Map.of("price", 100L, "quantity", 5L, "taxRate", 0.1));

            assertTrue(r.isSuccess(), "Errors: " + r.getErrors());
            assertEquals(500L, r.getResult("total"));
            assertEquals(50.0, r.getResult("tax"));
        }
    }

    @Nested
    @DisplayName("3-level nesting")
    class ThreeLevelNesting {
        @Test
        @DisplayName("depth 3 executes; depth 4 is rejected by dependency analysis")
        void depth3Ok() {
            // C -> B -> A: depths C=1, B=2, A=3 (within limit).
            Map<String, FormulaConfig> formulas = fm(
                "A", calc("B + 1", List.of("B")),
                "B", calc("C + 1", List.of("C")),
                "C", calc("42", Collections.emptyList())
            );

            ExecutionResult r = engine.execute(formulas, Map.of());
            assertTrue(r.isSuccess(), "Errors: " + r.getErrors());
            assertEquals(44L, r.getResult("A")); // C=42, B=43, A=44
        }

        @Test
        @DisplayName("depth 4 rejected by pre-check")
        void depth4Rejected() {
            Map<String, FormulaConfig> formulas = fm(
                "A", calc("B + 1", List.of("B")),
                "B", calc("C + 1", List.of("C")),
                "C", calc("D + 1", List.of("D")),
                "D", calc("42", Collections.emptyList())
            );

            // This should fail at the dependency analysis stage
            DependencyAnalysisResult dep = new com.mdframe.forge.plugin.generator.domain.formula.FormulaDependencyAnalyzer().analyze(formulas);
            assertFalse(dep.isValid(), "Depth 4 should be rejected");
        }
    }

    @Nested
    @DisplayName("error degradation")
    class ErrorDegradation {
        @Test
        @DisplayName("one formula fails, others still execute")
        void partialFailure() {
            Map<String, FormulaConfig> formulas = fm(
                "good", calc("10", Collections.emptyList()),
                "bad", calc("a +", List.of("a")),  // syntax error
                "alsoGood", calc("good + 5", List.of("good"))
            );

            ExecutionResult r = engine.execute(formulas, Map.of());

            // Overall failure because bad failed
            assertFalse(r.isSuccess());
            // But good and alsoGood should have executed
            assertEquals(10L, r.getResult("good"));
            assertEquals(15L, r.getResult("alsoGood"));
            // bad should have error
            assertTrue(r.hasError("bad"));
            assertNull(r.getResult("bad"));
        }

        @Test
        @DisplayName("missing variable degrades while remaining formulas continue")
        void missingVariable() {
            Map<String, FormulaConfig> formulas = fm(
                "A", calc("x + 1", List.of("x")),
                "B", calc("5", Collections.emptyList())
            );

            ExecutionResult r = engine.execute(formulas, Map.of());
            // A needs x which is missing
            assertFalse(r.isSuccess());
            assertTrue(r.hasError("A"));
            assertEquals(5L, r.getResult("B"));
        }
    }

    @Nested
    @DisplayName("cycle detection")
    class CycleDetection {
        @Test
        @DisplayName("A-B dependency cycle fails immediately")
        void cycle() {
            Map<String, FormulaConfig> formulas = fm(
                "A", calc("B + 1", List.of("B")),
                "B", calc("A * 2", List.of("A"))
            );

            ExecutionResult r = engine.execute(formulas, Map.of());
            assertFalse(r.isSuccess());
            assertTrue(r.getErrors().containsKey("DAG"));
        }
    }

    @Nested
    @DisplayName("analyze convenience")
    class Analyze {
        @Test
        @DisplayName("returns valid DependencyAnalysisResult for DAG")
        void validDag() {
            Map<String, FormulaConfig> formulas = fm(
                "A", calc("B + C", List.of("B", "C")),
                "B", calc("10", Collections.emptyList()),
                "C", calc("20", Collections.emptyList())
            );

            DependencyAnalysisResult r = new com.mdframe.forge.plugin.generator.domain.formula.FormulaDependencyAnalyzer().analyze(formulas);
            assertTrue(r.isValid());
            assertFalse(r.hasCycle());
            assertNotNull(r.getTopologicalOrder());
        }
    }

    @Nested
    @DisplayName("executeSingle convenience")
    class ExecuteSingle {
        @Test
        @DisplayName("executes single expression")
        void single() {
            Map<String, FormulaConfig> singleMap = Map.of("result", FormulaConfig.builder().type(FormulaType.CALC).mode(FormulaMode.STORED).expression("x * y").build());
            ExecutionResult execResult = engine.execute(singleMap, Map.of("x", 7L, "y", 6L));
            assertEquals(true, execResult.isSuccess());
            assertEquals(42L, execResult.getResult("result"));
        }
    }

    @Nested
    @DisplayName("aggregate formula execution (Phase 3B)")
    class AggregateFormulaExecution {
        @Test
        @DisplayName("AGGREGATE SUM executes successfully with default (empty) data provider")
        void aggregateSumDefault() {
            FormulaConfig aggConfig = FormulaConfig.builder()
                .type(FormulaType.AGGREGATE)
                .mode(FormulaMode.STORED)
                .expression("SUM(amount)")
                .aggregate(new AggregateConfig(AggregateFunction.SUM, "order_items", "amount", null))
                .build();

            Map<String, FormulaConfig> formulas = new LinkedHashMap<>();
            formulas.put("totalAmount", aggConfig);
            formulas.put("other", FormulaConfig.builder()
                .type(FormulaType.CALC)
                .mode(FormulaMode.STORED)
                .expression("42")
                .dependsOn(Collections.emptyList())
                .build());

            ExecutionResult r = engine.execute(formulas, Map.of());

            // Default AggregateEngine has an empty data provider, so SUM returns 0L.
            assertFalse(r.hasError("totalAmount"),
                "AGGREGATE should execute without error: " + r.getErrors());
            assertEquals(0L, r.getResult("totalAmount"));

            // Other formulas still execute
            assertEquals(42L, r.getResult("other"));
            assertTrue(r.isSuccess());
        }

        @Test
        @DisplayName("AGGREGATE with CALC: both succeed, dependencies resolved")
        void aggregateWithCalc() {
            Map<String, FormulaConfig> formulas = new LinkedHashMap<>();
            formulas.put("total", FormulaConfig.builder()
                .type(FormulaType.AGGREGATE)
                .mode(FormulaMode.STORED)
                .expression("SUM(detail.amount)")
                .aggregate(new AggregateConfig(AggregateFunction.SUM, "detail", "amount", null))
                .build());
            formulas.put("tax", FormulaConfig.builder()
                .type(FormulaType.CALC)
                .mode(FormulaMode.STORED)
                .expression("100 * 0.13")
                .dependsOn(Collections.emptyList())
                .build());

            ExecutionResult r = engine.execute(formulas, Map.of());

            assertFalse(r.hasError("total"), "AGGREGATE should succeed: " + r.getErrors());
            assertEquals(0L, r.getResult("total")); // no-op provider returns 0
            assertEquals(13.0, ((Number) r.getResult("tax")).doubleValue(), 0.001);
            assertTrue(r.isSuccess());
        }

        @Test
        @DisplayName("AGGREGATE + CALC chain: CALC depends on AGGREGATE result")
        void calcDependsOnAggregate() {
            Map<String, FormulaConfig> formulas = new LinkedHashMap<>();
            formulas.put("totalAmount", FormulaConfig.builder()
                .type(FormulaType.AGGREGATE)
                .mode(FormulaMode.STORED)
                .expression("SUM(amount)")
                .aggregate(new AggregateConfig(AggregateFunction.SUM, "items", "amount", null))
                .build());
            formulas.put("grandTotal", FormulaConfig.builder()
                .type(FormulaType.CALC)
                .mode(FormulaMode.STORED)
                .expression("totalAmount + tax")
                .dependsOn(List.of("totalAmount", "tax"))
                .build());

            ExecutionResult r = engine.execute(formulas,
                Map.of("tax", 50L));

            assertTrue(r.isSuccess(), "Errors: " + r.getErrors());
            assertEquals(0L, r.getResult("totalAmount")); // no-op provider
            assertEquals(50L, r.getResult("grandTotal"));  // 0 + 50
        }
    }
}
