package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.*;
import com.mdframe.forge.plugin.print.service.PrintDocumentAccess;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintLowcodeTestData.*;

class LowcodePrintValueAdapterTest {
    private final LowcodePrintValueAdapter adapter = new LowcodePrintValueAdapter(
            new LowcodePrintCatalogBuilder(new PrintDocumentAccess(JSON)), JSON);
    private PrintMetadataResolver.Model model() throws Exception {
        var config = new AiCrudConfig(); config.setDesensitizeConfig("{\"phone\":{\"type\":\"PHONE\"}}");
        return new PrintMetadataResolver.Model(config, JSON.readValue(PrintLowcodeTestData.model("test"), LowcodeModelSchema.class), new LowcodePageSchema());
    }
    @Test void preservesPrecisionAndUsesLabelsWithoutReplacingMasks() throws Exception {
        var data = adapter.adapt(model(), Map.of("id", 9007199254740993L, "amount", "12.30", "status", "A",
                "statusName", "已确认", "phone", "synthetic-mask", "phoneName", "must-not-leak", "secret", "hidden"),
                "main", Set.of("main.id", "main.amount", "main.status", "main.phone", "main.secret"));
        assertThat(data).containsEntry("id", "9007199254740993").containsEntry("amount", "1230")
                .containsEntry("status", "已确认").containsEntry("phone", "synthetic-mask").doesNotContainKey("secret");
        assertThat(data.values()).doesNotContain("must-not-leak");
    }
    @Test void preservesNullAndZeroAndRejectsFractionalCent() throws Exception {
        var row = new HashMap<String, Object>(); row.put("amount", "0.00"); row.put("status", null);
        var model = model();
        assertThat(adapter.adapt(model, row, "main", Set.of("main.amount", "main.status")))
                .containsEntry("amount", "0").containsEntry("status", null);
        assertThatThrownBy(() -> adapter.adapt(model, Map.of("amount", "0.001"), "main", Set.of("main.amount")))
                .hasMessageContaining("金额");
    }
    @Test void missingMaskPolicyFailsBeforeAnySensitiveValueIsReturned() throws Exception {
        var model = model(); model.config().setDesensitizeConfig(null);
        assertThatThrownBy(() -> adapter.adapt(model, Map.of("phone", "synthetic-sensitive"), "main", Set.of("main.phone")))
                .hasMessageContaining("脱敏");
    }
}
