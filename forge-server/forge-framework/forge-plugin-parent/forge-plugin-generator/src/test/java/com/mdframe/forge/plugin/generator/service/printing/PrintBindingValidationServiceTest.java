package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.*;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import com.mdframe.forge.plugin.print.service.PrintDocumentAccess;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintApplicationTestData.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintLowcodeTestData.*;

class PrintBindingValidationServiceTest {
    @Test void rejectsDeletedHiddenAndChangedFieldTypesWithFieldPath() throws Exception {
        var metadata = mock(PrintMetadataResolver.class);
        var model = JSON.readValue(model("synthetic"), LowcodeModelSchema.class);
        var resolved = new PrintMetadataResolver.Metadata(new PrintMetadataResolver.Model(new AiCrudConfig(), model,
                new LowcodePageSchema()), List.of());
        when(metadata.published(eq(ACTOR), eq(SOURCE), any())).thenReturn(resolved);
        var access = new PrintDocumentAccess(JSON);
        var resources = mock(LowcodePrintResourceAccess.class);
        var validator = new PrintBindingValidationService(metadata, new LowcodePrintCatalogBuilder(access),
                new PrintProtocolValidator(), access, resources, JSON);
        String schema = SCHEMA.replace("\"source\":\"CONSTANT\",\"value\":\"合成单据\"",
                "\"source\":\"FIELD\",\"path\":\"main.amount\"")
                .replace("\"kind\":\"TEXT\"", "\"kind\":\"TEXT\",\"format\":{\"type\":\"MONEY\"}");
        validator.validate(ACTOR, binding(10, true), schema, JSON.createObjectNode(), true);
        model.getFields().get(1).setBusinessFieldType("TEXT");
        model.getFields().get(1).setDataType("varchar");
        assertThatThrownBy(() -> validator.validate(ACTOR, binding(10, true), schema, JSON.createObjectNode(), true))
                .hasMessageContaining("main.amount");
        model.getFields().get(1).setFieldStatus("HIDDEN");
        assertThatThrownBy(() -> validator.validate(ACTOR, binding(10, true), schema, JSON.createObjectNode(), true))
                .hasMessageContaining("main.amount");
        model.getFields().remove(1);
        assertThatThrownBy(() -> validator.validate(ACTOR, binding(10, true), schema, JSON.createObjectNode(), true))
                .hasMessageContaining("main.amount");
        var flow = new PrintApplicationSnapshotCodec.Binding(SOURCE, PrintScene.FLOW_DONE, 10L, 20L, HASH, true, 0);
        assertThatThrownBy(() -> validator.validate(ACTOR, flow, SCHEMA, JSON.createObjectNode(), true))
                .hasMessageContaining("流程");
    }
}
