package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.print.entity.PrintTemplate;
import com.mdframe.forge.plugin.print.entity.PrintTemplateVersion;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateVersionMapper;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.mdframe.forge.plugin.generator.service.printing.PrintApplicationTestData.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrintApplicationVersionGuardTest {
    private final PrintIdentity identity = mock(PrintIdentity.class);
    private final PrintApplicationLock lock = mock(PrintApplicationLock.class);
    private final PrintTemplateMapper templates = mock(PrintTemplateMapper.class);
    private final PrintTemplateVersionMapper versions = mock(PrintTemplateVersionMapper.class);
    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final PrintApplicationVersionGuard guard = new PrintApplicationVersionGuard(identity, lock,
            new PrintApplicationSnapshotCodec(factory.getValidator()), templates, versions, new PrintProtocolValidator(),
            mock(PrintBindingValidationService.class), mock(PrintMetadataResolver.class));
    private final PrintTemplate template = new PrintTemplate();
    private final PrintTemplateVersion version = new PrintTemplateVersion();
    @BeforeEach void setup() {
        when(identity.current()).thenReturn(new PrintActor(1L, 9L, 1L));
        template.setId(10L); template.setApplicationId(2L); template.setStatus(1);
        template.setSourceType("LOWCODE"); template.setPageId("page_purchase");
        template.setObjectCode("purchase"); template.setSourceKey(SOURCE.key());
        template.setPublishedVersionId(99L); template.setDraftSchema("changed-invalid-draft");
        when(templates.lockScoped(1L, 10L)).thenReturn(template);
        version.setId(20L); version.setTemplateId(10L); version.setSchemaJson(SCHEMA); version.setSchemaHash(HASH);
        when(versions.selectScoped(1L, 10L, 20L)).thenReturn(version);
    }
    @AfterEach void close() { factory.close(); }

    @Test void usesPinnedHistoricalVersionAndLocksApplicationBeforeTemplate() {
        guard.lockAndValidate(2L, snapshot(binding(10, true)));
        var order = inOrder(lock, templates, versions);
        order.verify(lock).lock(1L, 2L);
        order.verify(templates).lockScoped(1L, 10L);
        order.verify(versions).selectScoped(1L, 10L, 20L);
        verify(versions, never()).selectScoped(1L, 10L, 99L);
    }
    @Test void disabledDeletedMovedOrWrongSourceTemplatesFailClosed() {
        template.setStatus(0);
        reject(); template.setStatus(1);
        template.setApplicationId(3L);
        reject(); template.setApplicationId(2L);
        template.setPageId("other_page");
        reject(); template.setPageId("page_purchase");
        when(templates.lockScoped(1L, 10L)).thenReturn(null);
        reject();
        verifyNoInteractions(versions);
    }
    @Test void missingHashMismatchAndTamperedVersionAreRejected() {
        version.setSchemaHash("a".repeat(64)); reject();
        version.setSchemaHash(HASH); version.setSchemaJson(SCHEMA.replace("合成单据", "changed")); reject();
        when(versions.selectScoped(1L, 10L, 20L)).thenReturn(null); reject();
    }
    @Test void legacyApplicationStillUsesSharedLockWithoutTemplateLookup() {
        guard.lockAndValidate(2L, "{}");
        verify(lock).lock(1L, 2L);
        verifyNoInteractions(templates, versions);
    }
    private void reject() {
        assertThatThrownBy(() -> guard.lockAndValidate(2L, snapshot(binding(10, true)))).isInstanceOf(BusinessException.class);
    }
}
