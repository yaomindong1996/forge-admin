package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.print.entity.*;
import com.mdframe.forge.plugin.print.mapper.*;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintApplicationTestData.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintLowcodeTestData.ACTOR;
import static com.mdframe.forge.plugin.generator.service.printing.PrintLowcodeTestData.JSON;

class PrintApplicationSnapshotContributorTest {
    @Test void capturesOnlyPublishedVersionAndKeepsCapturedReferenceAfterPointerChanges() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var identity = mock(PrintIdentity.class); when(identity.current()).thenReturn(ACTOR);
            var lock = mock(PrintApplicationLock.class);
            var bindings = mock(PrintBindingMapper.class);
            var templates = mock(PrintTemplateMapper.class);
            var versions = mock(PrintTemplateVersionMapper.class);
            var validation = mock(PrintBindingValidationService.class);
            var codec = new PrintApplicationSnapshotCodec(factory.getValidator());
            var service = new PrintApplicationSnapshotContributor(identity, lock, bindings, templates, versions,
                    new PrintProtocolValidator(), codec, JSON, validation);
            var row = new PrintBinding(); row.setApplicationId(2L); row.setSourceType("LOWCODE");
            row.setPageId(SOURCE.pageId()); row.setObjectCode("purchase"); row.setSourceKey(SOURCE.key());
            row.setTemplateId(10L); row.setScene("DETAIL"); row.setIsDefault(true); row.setSortOrder(0);
            when(bindings.selectApplication(1L, 2L)).thenReturn(List.of(row));
            var template = new PrintTemplate(); template.setId(10L); template.setApplicationId(2L);
            template.setSourceType("LOWCODE"); template.setPageId(SOURCE.pageId()); template.setObjectCode("purchase");
            template.setSourceKey(SOURCE.key()); template.setStatus(1); template.setPublishedVersionId(20L);
            template.setDraftSchema("invalid-new-draft"); when(templates.lockScoped(1L, 10L)).thenReturn(template);
            var version = new PrintTemplateVersion(); version.setId(20L); version.setSchemaJson(SCHEMA); version.setSchemaHash(HASH);
            when(versions.selectScoped(1L, 10L, 20L)).thenReturn(version);
            var captured = service.capture(2L, Map.of());
            template.setPublishedVersionId(21L);
            var pinned = codec.read(JSON.writeValueAsString(Map.of("printing", captured)), 2L);
            assertThat(pinned.get(0).templateVersionId()).isEqualTo(20L);
            assertThat(pinned.get(0).schemaHash()).isEqualTo(HASH);
            var order = inOrder(lock, bindings, templates, validation);
            order.verify(lock).lock(1L, 2L); order.verify(bindings).selectApplication(1L, 2L);
            order.verify(templates).lockScoped(1L, 10L);
            order.verify(validation).validate(eq(ACTOR), any(), eq(SCHEMA), any(), eq(false));
            assertThatThrownBy(() -> service.capture(2L, Map.of())).hasMessageContaining("版本");
            template.setStatus(0);
            assertThatThrownBy(() -> service.capture(2L, Map.of())).hasMessageContaining("模板");
        }
    }
}
