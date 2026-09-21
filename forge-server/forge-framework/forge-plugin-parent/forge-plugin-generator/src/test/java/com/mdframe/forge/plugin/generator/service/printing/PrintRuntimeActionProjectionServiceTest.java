package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationVersion;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigRenderVO;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationVersionMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationRuntimeService;
import com.mdframe.forge.plugin.generator.vo.businessapp.*;
import com.mdframe.forge.plugin.print.entity.PrintBinding;
import com.mdframe.forge.plugin.print.entity.PrintTemplate;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.mapper.PrintBindingMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.starter.core.session.SessionHelper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintApplicationTestData.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintLowcodeTestData.*;

class PrintRuntimeActionProjectionServiceTest {
    final PrintIdentity identity = mock(PrintIdentity.class);
    final BusinessApplicationRuntimeService runtime = mock(BusinessApplicationRuntimeService.class);
    final BusinessApplicationVersionMapper versions = mock(BusinessApplicationVersionMapper.class);
    final PrintTemplateMapper templates = mock(PrintTemplateMapper.class);
    final PrintBindingMapper bindings = mock(PrintBindingMapper.class);
    ValidatorFactory factory; MockedStatic<SessionHelper> session;
    PrintRuntimeActionProjectionService service; AiCrudConfigRenderVO config;
    BusinessApplicationRuntimeVO portal; PrintTemplate template; AiBusinessApplicationVersion version;
    @BeforeEach void setup() throws Exception {
        factory = Validation.buildDefaultValidatorFactory(); session = mockStatic(SessionHelper.class);
        session.when(() -> SessionHelper.hasPermission("print:execute")).thenReturn(true);
        session.when(() -> SessionHelper.hasPermission("ai:business:purchase:query")).thenReturn(true);
        when(identity.current()).thenReturn(ACTOR);
        var application = new BusinessApplicationVO(); application.setOptions(JSON.readTree(PrintLowcodeTestData.snapshot()).path("application").path("options").toString());
        var object = new BusinessApplicationObjectVO(); object.setObjectId(3L); object.setObjectCode("purchase"); object.setConfigKey("purchase");
        portal = new BusinessApplicationRuntimeVO(); portal.setApplication(application); portal.setObjects(List.of(object)); portal.setVersionNo(1);
        when(runtime.runtimeById(2L)).thenReturn(portal);
        version = new AiBusinessApplicationVersion(); version.setSnapshotJson(PrintApplicationTestData.snapshot(binding(10, true),
                new PrintApplicationSnapshotCodec.Binding(SOURCE, PrintScene.LIST, 10L, 20L, HASH, true, 0)));
        when(versions.selectVersion(1L, 2L, 1)).thenReturn(version);
        template = new PrintTemplate(); template.setApplicationId(2L); template.setSourceKey(SOURCE.key()); template.setStatus(1);
        when(templates.selectScoped(1L, 10L)).thenReturn(template);
        config = new AiCrudConfigRenderVO(); config.setRowKey("documentKey"); config.setOptions(Map.of("runtimeActions", List.of(Map.of("key", "existing", "position", "row"))));
        config.setColumnsSchema(List.of(Map.of("key", "name", "title", "名称")));
        service = new PrintRuntimeActionProjectionService(identity, runtime, versions, new PrintApplicationSnapshotCodec(factory.getValidator()),
                new LowcodePrintSourceResolver(), templates, bindings, JSON);
    }
    @AfterEach void close() { session.close(); factory.close(); }
    @Test void projectsBothScenesAndPreservesExistingActionsWithoutRowPayload() throws Exception {
        service.overlay("purchase", 2L, "page_purchase", config, false);
        service.overlay("purchase", 2L, "page_purchase", config, false);
        var root = JSON.valueToTree(config); var actions = root.path("options").path("runtimeActions");
        assertThat(actions.size()).isEqualTo(3);
        assertThat(actions.get(1).path("routePath").asText()).isEqualTo("/print/preview");
        assertThat(actions.get(1).path("params").get(5).path("sourceField").asText()).isEqualTo("documentKey");
        assertThat(actions.get(1).path("position").asText()).isEqualTo("detail");
        assertThat(actions.get(2).path("position").asText()).isEqualTo("row");
        assertThat(root.path("columnsSchema").size()).isEqualTo(2);
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target"));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/print-runtime-actions.json"), JSON.writeValueAsString(actions));
        verifyNoInteractions(bindings);
    }
    @Test void designPreviewProjectsLiveBindingsWithoutReadingSnapshot() {
        var row = new PrintBinding();
        row.setApplicationId(2L);
        row.setSourceType("LOWCODE");
        row.setPageId("page_purchase");
        row.setObjectCode("purchase");
        row.setSourceKey(SOURCE.key());
        row.setTemplateId(10L);
        row.setScene("LIST");
        row.setStatus(1);
        when(bindings.selectApplicationEnabled(1L, 2L)).thenReturn(List.of(row));
        service.overlay("purchase", 2L, "page_purchase", config, true);
        var actions = JSON.valueToTree(config.getOptions()).path("runtimeActions");
        assertThat(actions.size()).isEqualTo(2);
        assertThat(actions.get(1).path("key").asText()).isEqualTo("forgePrint:LIST");
        assertThat(actions.get(1).path("position").asText()).isEqualTo("row");
        verifyNoInteractions(versions);
        verify(bindings).selectApplicationEnabled(1L, 2L);
    }
    @Test void noPrintPermissionDoesNotReadApplication() {
        session.when(() -> SessionHelper.hasPermission("print:execute")).thenReturn(false);
        service.overlay("purchase", 2L, "page_purchase", config, false);
        service.overlay("purchase", 2L, "page_purchase", config, true);
        verifyNoInteractions(runtime, versions, templates, bindings);
    }
    @Test void unrelatedPageOrConfigCannotReuseAnotherPageBinding() {
        service.overlay("purchase", 2L, "page_other", config, false);
        service.overlay("other", 2L, "page_purchase", config, false);
        verifyNoInteractions(templates); assertOriginal();
    }
    @Test void portalFilteredPageAndRevokedObjectPermissionHideAction() {
        portal.getApplication().setOptions("{\"inAppBuilder\":{\"nodes\":[],\"pages\":{}}}");
        service.overlay("purchase", 2L, "page_purchase", config, false); assertOriginal();
        session.when(() -> SessionHelper.hasPermission("ai:business:purchase:query")).thenReturn(false);
        service.overlay("purchase", 2L, "page_purchase", config, false); assertOriginal();
        verifyNoInteractions(templates);
    }
    @Test void stoppedTemplateAndMissingSnapshotNeverFallbackToDraftBindings() {
        template.setStatus(0); service.overlay("purchase", 2L, "page_purchase", config, false); assertOriginal();
        version.setSnapshotJson("{}"); service.overlay("purchase", 2L, "page_purchase", config, false); assertOriginal();
        verifyNoInteractions(bindings);
    }
    @Test void invalidManifestIsNotSilentlyAccepted() {
        version.setSnapshotJson("{\"printing\":null}");
        assertThatThrownBy(() -> service.overlay("purchase", 2L, "page_purchase", config, false)).isInstanceOf(RuntimeException.class);
    }
    private void assertOriginal() { assertThat(JSON.valueToTree(config.getOptions()).path("runtimeActions").size()).isEqualTo(1); }
}
