package com.mdframe.forge.plugin.generator.service.printing;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.generator.mapper.*;
import com.mdframe.forge.plugin.print.service.PrintDocumentAccess;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintLowcodeTestData.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintApplicationTestData.SOURCE;

class PrintMetadataResolverTest {
    final AiCrudConfigMapper drafts = mock(AiCrudConfigMapper.class);
    final BusinessObjectDesignVersionMapper designs = mock(BusinessObjectDesignVersionMapper.class);
    final AiCrudConfigVersionMapper versions = mock(AiCrudConfigVersionMapper.class);
    final PrintMetadataResolver resolver = new PrintMetadataResolver(JSON, new LowcodePrintSourceResolver(),
            mock(BusinessApplicationMapper.class), mock(BusinessApplicationObjectMapper.class), drafts, designs, versions);
    @BeforeEach void setup() {
        when(designs.selectVersionById(1L, 3L, 30L)).thenReturn(design(30, 3, 13, 130, "purchase"));
        when(designs.selectVersionById(1L, 4L, 40L)).thenReturn(design(40, 4, 14, 140, "item"));
        when(versions.selectVersionById(1L, 13L, 130L)).thenReturn(version(130, 13, "purchase", true));
        when(versions.selectVersionById(1L, 14L, 140L)).thenReturn(version(140, 14, "item", false));
    }
    @Test void pinsBothConfigsAndNeverReadsDraftOrLatestPointers() {
        var result = resolver.published(ACTOR, SOURCE, resolver.parse(snapshot()));
        assertThat(result.main().config().getId()).isEqualTo(13);
        assertThat(result.children()).hasSize(1);
        assertThat(result.children().get(0).childColumn()).isEqualTo("purchase_id");
        assertThat(result.children().get(0).mainColumn()).isEqualTo("id");
        assertThat(result.children().get(0).model().config().getDesensitizeConfig()).contains("PHONE");
        verifyNoInteractions(drafts);
        var catalog = new LowcodePrintCatalogBuilder(new PrintDocumentAccess(JSON)).build(result);
        assertThat(catalog.fields()).extracting(field -> field.path()).contains("main.amount", "children.items.phone")
                .doesNotContain("main.secret", "main.password", "children.items.id", "children.items.secret");
    }
    @Test void draftDesignVersionCannotMasqueradeAsPublishedObject() {
        var draft = design(30, 3, 13, 130, "purchase"); draft.setPublishStatus("DRAFT");
        when(designs.selectVersionById(1L, 3L, 30L)).thenReturn(draft);
        reject(snapshot());
        verifyNoInteractions(versions, drafts);
    }
    @Test void missingPublishedVersionOrSecuritySnapshotNeverFallsBack() {
        when(versions.selectVersionById(1L, 14L, 140L)).thenReturn(null);
        reject(snapshot());
        var v = version(140, 14, "item", false); v.setPublishSnapshot("{}");
        when(versions.selectVersionById(1L, 14L, 140L)).thenReturn(v);
        reject(snapshot());
        verifyNoInteractions(drafts);
    }
    @Test void unmatchedChildModelRefDoesNotFailTheSource() {
        var result = resolver.published(ACTOR, SOURCE, resolver.parse(snapshot()
                .replace("\"objectCode\":\"item\"", "\"objectCode\":\"other\"")
                .replace("\"configKey\":\"item\"", "\"configKey\":\"other\"")
                .replace("\"tableName\":\"test_item\"", "\"tableName\":\"unrelated\"")));
        assertThat(result.main().config().getId()).isEqualTo(13);
        assertThat(result.children()).isEmpty();
    }

    @Test void childMatchesByObjectCodeWhenTableNameDiffers() {
        var main = version(130, 13, "purchase", true);
        main.setPageSchema(page(true)
                .replace("\"modelCode\":\"items\"", "\"modelCode\":\"item\"")
                .replace("\"tableName\":\"test_item\"", "\"tableName\":\"other_table\""));
        when(versions.selectVersionById(1L, 13L, 130L)).thenReturn(main);
        var result = resolver.published(ACTOR, SOURCE, resolver.parse(snapshot()
                .replace("\"tableName\":\"test_item\"", "\"tableName\":\"runtime_item\"")));
        assertThat(result.children()).hasSize(1);
        assertThat(result.children().get(0).key()).isEqualTo("item");
    }

    @Test void movedSourceOrMissingChildObjectIsRejected() {
        reject(snapshot().replace("\"objectId\":\"3\",\"objectCode\":\"purchase\",\"configKey\":\"purchase\"}",
                "\"objectId\":\"4\",\"objectCode\":\"purchase\",\"configKey\":\"purchase\"}"));
        reject(snapshot().replace("\"publishedDesignVersionId\":\"40\"", "\"publishedDesignVersionId\":null"));
    }
    @Test void guessesNoForeignKeyWhenRelationIsBroken() {
        var v = version(130, 13, "purchase", true);
        v.setPageSchema(page(true).replace("purchase_id", "missing_foreign_key"));
        when(versions.selectVersionById(1L, 13L, 130L)).thenReturn(v);
        reject(snapshot());
    }
    @Test void publishedChildDisplayColumnsAndMainPageFieldsFurtherRestrictCatalog() {
        var v = version(130, 13, "purchase", true);
        v.setOptions("""
            {"masterDetailConfig":{"children":[{"modelCode":"items","tableName":"test_item",
              "sourceField":"purchaseId","targetField":"id","fields":[{"sourceField":"amount"}]}]}}
            """);
        v.setPageSchema(page(true).replace("\"primaryModelCode\":\"purchase\"",
                "\"zones\":[{\"zoneKey\":\"detail\",\"fieldRefs\":[\"amount\"]}],\"primaryModelCode\":\"purchase\""));
        when(versions.selectVersionById(1L, 13L, 130L)).thenReturn(v);
        var result = resolver.published(ACTOR, SOURCE, resolver.parse(snapshot()));
        var catalog = new LowcodePrintCatalogBuilder(new PrintDocumentAccess(JSON)).build(result);
        assertThat(catalog.fields()).extracting(field -> field.path())
                .containsExactly("main.amount", "children.items", "children.items.amount");
    }
    @Test void filteredEmptyPageTreeCannotUseLegacyFallback() {
        var root = resolver.parse(snapshot());
        var options = (ObjectNode) root.path("application").path("options");
        options.put("primaryObjectCode", "purchase");
        options.set("inAppBuilder", JSON.createObjectNode());
        assertThatThrownBy(() -> new LowcodePrintSourceResolver().object(root, SOURCE, false)).isInstanceOf(BusinessException.class);
    }
    @Test void runtimeDisableCheckUsesOnlyStatusQueryWithoutDraftFallback() {
        var result = resolver.published(ACTOR, SOURCE, resolver.parse(snapshot()));
        when(drafts.countActiveRuntimeConfig(1L, 13L, "purchase")).thenReturn(1L);
        when(drafts.countActiveRuntimeConfig(1L, 14L, "item")).thenReturn(1L);
        resolver.assertRuntimeEnabled(ACTOR, result);
        when(drafts.countActiveRuntimeConfig(1L, 14L, "item")).thenReturn(0L);
        assertThatThrownBy(() -> resolver.assertRuntimeEnabled(ACTOR, result)).isInstanceOf(BusinessException.class);
        verify(drafts, never()).selectByConfigKey(anyLong(), anyString());
    }
    private void reject(String snapshot) {
        assertThatThrownBy(() -> resolver.published(ACTOR, SOURCE, resolver.parse(snapshot))).isInstanceOf(BusinessException.class);
    }
}
