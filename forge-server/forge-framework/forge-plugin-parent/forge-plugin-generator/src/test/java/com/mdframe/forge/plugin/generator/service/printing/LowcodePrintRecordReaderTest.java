package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.*;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.print.service.PrintDocumentAccess;
import com.mdframe.forge.plugin.print.spi.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintLowcodeTestData.*;

class LowcodePrintRecordReaderTest {
    @Test void childRelationUsesSavedRawKeyAndOnlyRequestedCollectionsAndFieldsAreReturned() throws Exception {
        var crud = mock(DynamicCrudService.class);
        var config = new AiCrudConfig(); config.setDesensitizeConfig("{\"phone\":{\"type\":\"PHONE\"}}");
        var model = new PrintMetadataResolver.Model(config, JSON.readValue(model("test"), LowcodeModelSchema.class), new LowcodePageSchema());
        var data = new PrintMetadataResolver.Metadata(model, List.of(new PrintMetadataResolver.Child("items", model, "id", "purchase_id", List.of())));
        var reader = new LowcodePrintRecordReader(crud, new LowcodePrintValueAdapter(new LowcodePrintCatalogBuilder(new PrintDocumentAccess(JSON)), JSON));
        when(crud.selectPrintById(config, "record")).thenReturn(new DynamicCrudService.PrintRow(Map.of("id", 7L), Map.of("id", "masked-id", "amount", "12.00")));
        when(crud.selectPrintChildren(config, "purchase_id", 7L)).thenReturn(List.of(Map.of("amount", "2.30", "secret", "never-return")));
        var selection = new PrintBindingSelection(new AuthorizedPrintContext.VersionRef(10L, 20L, true, 0),
                Set.of("main.amount", "children.items.amount"), Set.of("children.items"), Set.of());
        var result = reader.read(data, "record", selection);
        assertThat(result.main()).containsOnlyKeys("amount").containsEntry("amount", "1200");
        assertThat(result.children().get("items")).isEqualTo(List.of(Map.of("amount", "230")));
        verify(crud).selectPrintChildren(config, "purchase_id", 7L);
        when(crud.selectPrintById(config, "record")).thenReturn(null);
        assertThatThrownBy(() -> reader.read(data, "record", selection)).isInstanceOf(RuntimeException.class);
    }
}
