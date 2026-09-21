package com.mdframe.forge.plugin.generator.service.formula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.formula.*;
import com.mdframe.forge.plugin.generator.dto.lowcode.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VirtualFormulaPrintTest {
    LowcodeModelSchema schema(String expression) {
        var schema = new LowcodeModelSchema(); var field = new LowcodeFieldSchema(); field.setField("total");
        field.setFormulaConfig(Map.of("type", "CALC", "mode", "VIRTUAL", "expression", expression,
                "dependsOn", List.of("price", "quantity")));
        schema.setFields(List.of(field)); return schema;
    }
    @Test void realEngineCalculatesWithoutWritingExecutionLogs() {
        var logs = mock(FormulaExecutionLogService.class);
        var runtime = new VirtualFormulaRuntime(new FormulaExecutionEngine(), new ObjectMapper(), logs);
        var row = new LinkedHashMap<String, Object>(Map.of("price", 100L, "quantity", 5L));
        runtime.calculateForPrint(List.of(row), schema("price * quantity"), new FormulaRuntimeContext(1L, "test", "item", row));
        assertThat(row.get("total")).isEqualTo(500L);
        verifyNoInteractions(logs);
    }
    @Test void anyFormulaFailureStopsWholeOutputWithoutReturningPartialResultsOrInputs() {
        var engine = mock(FormulaExecutionEngine.class); var logs = mock(FormulaExecutionLogService.class);
        when(engine.execute(anyMap(), anyMap(), any())).thenReturn(ExecutionResult.builder().success(false)
                .putResult("total", 3L).putError("total", "synthetic-private-input").build());
        var runtime = new VirtualFormulaRuntime(engine, new ObjectMapper(), logs);
        var row = new LinkedHashMap<String, Object>(Map.of("price", 100L));
        assertThatThrownBy(() -> runtime.calculateForPrint(List.of(row), schema("price * quantity"),
                new FormulaRuntimeContext(1L, "test", "item", row)))
                .hasMessageContaining("total").hasMessageNotContaining("synthetic-private-input");
        assertThat(row).doesNotContainKey("total");
        verify(engine).execute(anyMap(), anyMap(), same(FormulaTraceOptions.disabled()));
        verifyNoInteractions(logs);
    }
}
