package com.mdframe.forge.plugin.generator.service.printing;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.generator.domain.entity.*;
import com.mdframe.forge.plugin.generator.dto.lowcode.*;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationVersionMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationRuntimeService;
import com.mdframe.forge.plugin.generator.vo.businessapp.*;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.service.*;
import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;
import com.mdframe.forge.starter.core.session.SessionHelper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintApplicationTestData.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintLowcodeTestData.ACTOR;
import static com.mdframe.forge.plugin.generator.service.printing.PrintLowcodeTestData.JSON;

class LowcodePrintDataProviderTest {
    final PrintIdentity identity = mock(PrintIdentity.class);
    final BusinessApplicationRuntimeService runtime = mock(BusinessApplicationRuntimeService.class);
    final BusinessApplicationVersionMapper versions = mock(BusinessApplicationVersionMapper.class);
    final com.mdframe.forge.plugin.print.mapper.PrintTemplateVersionMapper templateVersions = mock(com.mdframe.forge.plugin.print.mapper.PrintTemplateVersionMapper.class);
    final PrintMetadataResolver metadata = mock(PrintMetadataResolver.class);
    final LowcodePrintRecordReader records = mock(LowcodePrintRecordReader.class);
    final FlowPrintContextResolver flowContexts = mock(FlowPrintContextResolver.class);
    final FlowPrintHistoryAdapter flowHistory = mock(FlowPrintHistoryAdapter.class);
    final PrintRecordRequest request = new PrintRecordRequest(SOURCE, "saved-record", PrintScene.DETAIL, null, null, null);
    ValidatorFactory factory;
    MockedStatic<SessionHelper> session;
    LowcodePrintDataProvider provider;
    BusinessApplicationRuntimeVO portal;
    @BeforeEach void setup() throws Exception {
        factory = Validation.buildDefaultValidatorFactory();
        session = mockStatic(SessionHelper.class);
        session.when(() -> SessionHelper.hasPermission("ai:business:purchase:query")).thenReturn(true);
        when(identity.require("print:execute")).thenReturn(ACTOR); when(identity.current()).thenReturn(ACTOR);
        var object = new BusinessApplicationObjectVO(); object.setObjectId(3L); object.setObjectCode("purchase"); object.setConfigKey("purchase");
        var application = new BusinessApplicationVO(); application.setId(2L);
        application.setOptions(JSON.readTree(PrintLowcodeTestData.snapshot()).path("application").path("options").toString());
        portal = new BusinessApplicationRuntimeVO(); portal.setApplication(application); portal.setObjects(List.of(object)); portal.setVersionNo(1);
        when(runtime.runtimeById(2L)).thenReturn(portal);
        var version = new AiBusinessApplicationVersion(); version.setId(100L); version.setSnapshotJson(snapshot(binding(10, true)));
        when(versions.selectVersion(1L, 2L, 1)).thenReturn(version);
        when(metadata.parse(anyString())).thenAnswer(call -> JSON.readTree(call.getArgument(0, String.class)));
        var model = new LowcodeModelSchema(); var f = new LowcodeFieldSchema(); f.setField("id"); f.setDataType("bigint"); model.setFields(List.of(f));
        var data = new PrintMetadataResolver.Metadata(new PrintMetadataResolver.Model(new AiCrudConfig(), model, new LowcodePageSchema()), List.of());
        when(metadata.published(eq(ACTOR), eq(SOURCE), any())).thenReturn(data);
        var pinned = new com.mdframe.forge.plugin.print.entity.PrintTemplateVersion(); pinned.setSchemaHash(HASH);
        when(templateVersions.selectScoped(1L, 10L, 20L)).thenReturn(pinned);
        when(flowHistory.catalog()).thenReturn(List.of(
                new PrintFieldCatalogVO.Field("flow.processInstanceId", "流程实例ID", "TEXT"),
                new PrintFieldCatalogVO.Field("flow.history", "审批记录", "COLLECTION"),
                new PrintFieldCatalogVO.Field("flow.history.taskName", "节点名称", "TEXT")));
        when(flowHistory.empty()).thenAnswer(ignored -> {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("processInstanceId", null);
            empty.put("businessKey", null);
            empty.put("taskDefKey", null);
            empty.put("history", List.of());
            return empty;
        });
        var access = new PrintDocumentAccess(JSON);
        provider = new LowcodePrintDataProvider(identity, mock(PrintApplicationAccessAdapter.class), runtime, versions,
                new PrintApplicationSnapshotCodec(factory.getValidator()), metadata, new LowcodePrintSourceResolver(),
                new LowcodePrintCatalogBuilder(access), records, mock(LowcodePrintResourceAccess.class), access, JSON,
                templateVersions, mock(com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper.class),
                mock(com.mdframe.forge.plugin.print.mapper.PrintBindingMapper.class),
                flowContexts, mock(FlowPrintAccessPolicy.class), flowHistory);
    }
    @AfterEach void close() { session.close(); factory.close(); }
    @Test void runtimeNeedsNoDesignPermissionAndUsesOnlySnapshotVersion() {
        var context = provider.authorize(ACTOR, request);
        assertThat(context.applicationVersionId()).isEqualTo(100);
        assertThat(context.versions().get(0).templateVersionId()).isEqualTo(20);
        assertThat(context.catalog().fields()).extracting(PrintFieldCatalogVO.Field::path)
                .contains("flow.history", "flow.history.taskName");
        verify(identity).require("print:execute");
        verifyNoMoreInteractions(identity);
        verify(records).assertReadable(any(), eq("saved-record"));
        verify(metadata, never()).draft(any(), any());
        verify(flowContexts).resolveForRecord(ACTOR, request);
    }
    @Test void deniedPageOrObjectPermissionStopsBeforeRecordRead() {
        portal.getApplication().setOptions("{\"primaryObjectCode\":\"purchase\",\"inAppBuilder\":{\"nodes\":[],\"pages\":{}}}");
        assertThatThrownBy(() -> provider.authorize(ACTOR, request)).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(records);
    }
    @Test void revokedObjectPermissionAndOtherActorAreDenied() {
        session.when(() -> SessionHelper.hasPermission("ai:business:purchase:query")).thenReturn(false);
        assertThatThrownBy(() -> provider.authorize(ACTOR, request)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> provider.authorize(new PrintActor(2L, 9L, 1L), request)).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(records);
    }
    @Test void inaccessibleRecordIsNotReportedAsPrintable() {
        doThrow(PrintFailure.missing()).when(records).assertReadable(any(), anyString());
        assertThatThrownBy(() -> provider.authorize(ACTOR, request)).isInstanceOf(RuntimeException.class);
    }
    @Test void changedApplicationOrInjectedVersionOrFieldIsRejectedOnLoad() {
        var context = provider.authorize(ACTOR, request);
        var valid = context.versions().get(0);
        assertThatThrownBy(() -> provider.load(context, new PrintBindingSelection(valid, Set.of("main.secret"), Set.of(), Set.of())))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> provider.load(context, new PrintBindingSelection(new AuthorizedPrintContext.VersionRef(10L, 999L, true, 0),
                Set.of(), Set.of(), Set.of()))).isInstanceOf(RuntimeException.class);
        portal.setVersionNo(2);
        assertThatThrownBy(() -> provider.load(context, new PrintBindingSelection(valid, Set.of("main.id"), Set.of(), Set.of())))
                .hasMessageContaining("应用发布版本");
        verify(records, never()).read(any(), anyString(), any());
    }
    @Test void detailPrintCanLoadEmptyFlowHistoryWithoutProcessRun() {
        var context = provider.authorize(ACTOR, request);
        when(records.read(any(), eq("saved-record"), any())).thenReturn(new PrintData(Map.of("id", 1), Map.of(), Map.of()));
        var data = provider.load(context, new PrintBindingSelection(context.versions().get(0),
                Set.of("flow.history.taskName"), Set.of("flow.history"), Set.of()));
        assertThat(data.flow()).containsEntry("history", List.of());
        verify(flowHistory).empty();
        verify(flowHistory, never()).load(any());
    }
    @Test void corruptPinnedHashIsRejectedAgainstApplicationManifest() {
        var pinned = new com.mdframe.forge.plugin.print.entity.PrintTemplateVersion(); pinned.setSchemaHash("a".repeat(64));
        when(templateVersions.selectScoped(1L, 10L, 20L)).thenReturn(pinned);
        assertThatThrownBy(() -> provider.authorize(ACTOR, request)).hasMessageContaining("版本校验");
        verifyNoInteractions(records);
    }
    @Test void flowScenesStayClosedUntilM5() {
        var flow = new PrintRecordRequest(SOURCE, "record", PrintScene.FLOW_DONE, "task", "process", 1L);
        assertThatThrownBy(() -> provider.authorize(ACTOR, flow)).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(runtime, records);
    }
}
