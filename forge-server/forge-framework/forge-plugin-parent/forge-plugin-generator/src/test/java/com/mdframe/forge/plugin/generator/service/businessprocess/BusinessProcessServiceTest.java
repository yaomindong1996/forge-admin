package com.mdframe.forge.plugin.generator.service.businessprocess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.businessprocess.schema.BusinessProcessSchema;
import com.mdframe.forge.plugin.generator.businessprocess.validation.BusinessProcessSchemaValidator;
import com.mdframe.forge.plugin.generator.businessprocess.validation.BusinessProcessValidationContext;
import com.mdframe.forge.plugin.generator.businessprocess.validation.BusinessProcessValidationContextResolver;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcess;
import com.mdframe.forge.plugin.generator.dto.businessprocess.BusinessProcessDTO;
import com.mdframe.forge.plugin.generator.dto.businessprocess.BusinessProcessSchemaDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessProcessMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessProcessRunMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessProcessVersionMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessNamingService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessprocess.BusinessObjectProcessVO;
import com.mdframe.forge.plugin.generator.vo.businessprocess.BusinessProcessVO;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BusinessProcessService")
class BusinessProcessServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BusinessProcessMapper processMapper = mock(BusinessProcessMapper.class);
    private final BusinessProcessVersionMapper versionMapper = mock(BusinessProcessVersionMapper.class);
    private final BusinessProcessRunMapper runMapper = mock(BusinessProcessRunMapper.class);
    private final BusinessApplicationMapper applicationMapper = mock(BusinessApplicationMapper.class);
    private final BusinessApplicationObjectMapper applicationObjectMapper = mock(BusinessApplicationObjectMapper.class);
    private final BusinessProcessValidationContextResolver contextResolver =
            mock(BusinessProcessValidationContextResolver.class);
    private final BusinessProcessSchemaValidator validator = new BusinessProcessSchemaValidator(objectMapper);
    private final BusinessProcessService service = new BusinessProcessService(
            processMapper,
            versionMapper,
            runMapper,
            applicationMapper,
            applicationObjectMapper,
            validator,
            contextResolver,
            new BusinessNamingService());
    private final BusinessFlowService businessFlowService = mock(BusinessFlowService.class);

    private ExecutionIdentityContextHolder.Scope identityScope;

    @BeforeEach
    void setUp() {
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        user.setPermissions(Set.of("ai:businessProcess:start"));
        identityScope = ExecutionIdentityContextHolder.open(new ExecutionIdentity(
                user, "USER", 101L, null, 301L,
                "business_process_test", "token-test", Set.of()));
        try {
            Field field = BusinessProcessService.class.getDeclaredField("businessFlowService");
            field.setAccessible(true);
            field.set(service, businessFlowService);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @AfterEach
    void tearDown() {
        identityScope.close();
    }

    @Test
    @DisplayName("create validates application ownership and initializes a canonical draft")
    void createInitializesCanonicalDraft() {
        when(applicationMapper.selectEntityById(1L, 10L)).thenReturn(application());
        when(applicationObjectMapper.selectByApplicationId(1L, 10L)).thenReturn(List.of(applicationObject()));
        when(processMapper.selectActiveByCode(eq(1L), eq(10L), any())).thenReturn(null);
        when(processMapper.insert(any(AiBusinessProcess.class))).thenAnswer(invocation -> {
            AiBusinessProcess process = invocation.getArgument(0);
            process.setId(1001L);
            return 1;
        });
        BusinessProcessDTO dto = new BusinessProcessDTO();
        dto.setApplicationId("10");
        dto.setProcessName("订单审批流程");
        dto.setSubjectObjectId("20");

        BusinessProcessVO result = service.create(dto);

        ArgumentCaptor<AiBusinessProcess> captor = ArgumentCaptor.forClass(AiBusinessProcess.class);
        verify(processMapper).insert(captor.capture());
        AiBusinessProcess inserted = captor.getValue();
        BusinessProcessSchema schema = validator.normalize(inserted.getDraftSchemaJson());
        assertEquals("1001", result.getId());
        assertEquals("10", result.getApplicationId());
        assertEquals("20", result.getSubjectObjectId());
        assertEquals(inserted.getProcessCode(), schema.getProcessCode());
        assertEquals(1, schema.getNodes().stream().filter(node -> node.getType().startsWith("START_")).count());
        assertEquals(64, inserted.getDraftSchemaHash().length());
        assertEquals("DRAFT", inserted.getDesignStatus());
        assertNull(inserted.getPublishedVersion());
    }

    @Test
    @DisplayName("object process summary keeps tenant and canonical object code scope")
    void listByObjectCodeKeepsTenantAndObjectScope() {
        BusinessObjectProcessVO summary = new BusinessObjectProcessVO();
        summary.setId("1001");
        summary.setProcessCode("order_created_update");
        summary.setStartNodeType("START_EVENT");
        when(processMapper.selectBySubjectObjectCode(1L, "order")).thenReturn(List.of(summary));

        List<BusinessObjectProcessVO> result = service.listByObjectCode("  order  ");

        assertEquals(1, result.size());
        assertEquals("1001", result.get(0).getId());
        assertEquals("START_EVENT", result.get(0).getStartNodeType());
        verify(processMapper).selectBySubjectObjectCode(1L, "order");
    }

    @Test
    @DisplayName("summary extracts the actual draft start node and never exposes draft JSON")
    void listByObjectCodeExtractsStartAndHidesDraft() {
        BusinessObjectProcessVO summary = new BusinessObjectProcessVO();
        summary.setId("1001");
        summary.setDraftSchemaJson("""
                {"nodes":[{"id":"end","type":"END"},
                  {"id":"start","type":"START_EVENT"}],"edges":[]}
                """);
        when(processMapper.selectBySubjectObjectCode(1L, "order")).thenReturn(List.of(summary));
        var result = service.listByObjectCode("order");
        assertEquals("START_EVENT", result.get(0).getStartNodeType());
        assertNull(result.get(0).getDraftSchemaJson());
        verify(processMapper).selectBySubjectObjectCode(1L, "order");
    }

    @Test
    @DisplayName("process code is immutable after creation")
    void processCodeIsImmutable() {
        when(processMapper.selectActiveById(1L, 1001L)).thenReturn(process());
        BusinessProcessDTO dto = new BusinessProcessDTO();
        dto.setId("1001");
        dto.setApplicationId("10");
        dto.setSubjectObjectId("20");
        dto.setProcessCode("changed_code");
        dto.setProcessName("修改名称");

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(dto));

        assertEquals("流程编码创建后不能修改", error.getMessage());
        verify(processMapper, never()).updateBasicInfo(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("draft save reports HTTP 409 when the expected schema hash is stale")
    void staleSchemaHashReturnsConflict() throws Exception {
        AiBusinessProcess process = process();
        process.setDraftSchemaHash("a".repeat(64));
        when(processMapper.selectActiveById(1L, 1001L)).thenReturn(process);
        BusinessProcessSchema schema = validator.normalize(validSchema("order_created_update"));
        BusinessProcessValidationContext context = validationContext("order_created_update");
        when(contextResolver.resolve(1L, 10L, "order_created_update", schema)).thenReturn(context);
        when(processMapper.updateDraftSchema(
                eq(1L), eq(1001L), any(), any(), eq("a".repeat(64)),
                eq(20L), eq("order"), any(), eq(101L))).thenReturn(0);
        BusinessProcessSchemaDTO dto = new BusinessProcessSchemaDTO();
        dto.setExpectedSchemaHash("a".repeat(64));
        dto.setBusinessProcessJson(objectMapper.readTree(validSchema("order_created_update")));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveSchema(1001L, dto));

        assertEquals(409, error.getCode());
        assertTrue(error.getMessage().contains("草稿已被其他操作更新"));
    }

    @Test
    @DisplayName("structurally incomplete drafts remain saveable and stay in draft status")
    void incompleteDraftCanBeSaved() throws Exception {
        AiBusinessProcess process = process();
        String expectedHash = process.getDraftSchemaHash();
        when(processMapper.selectActiveById(1L, 1001L)).thenReturn(process);
        when(contextResolver.resolve(eq(1L), eq(10L), eq("order_created_update"), any()))
                .thenReturn(validationContext("order_created_update"));
        when(processMapper.updateDraftSchema(
                eq(1L), eq(1001L), any(), any(), eq(expectedHash),
                eq(20L), eq("order"), eq("DRAFT"), eq(101L))).thenReturn(1);
        BusinessProcessSchemaDTO dto = new BusinessProcessSchemaDTO();
        dto.setExpectedSchemaHash(expectedHash);
        dto.setBusinessProcessJson(objectMapper.readTree(incompleteSchema()));

        BusinessProcessVO result = service.saveSchema(1001L, dto);

        assertEquals("DRAFT", result.getDesignStatus());
        assertFalse(result.getValidation().isValid());
        assertTrue(result.getValidation().getIssues().stream()
                .anyMatch(issue -> "END_NODE_REQUIRED".equals(issue.getCode())));
        verify(applicationMapper).markChanged(1L, 10L);
    }

    @Test
    @DisplayName("draft save rejects a subject object outside the current application")
    void crossApplicationSubjectIsRejected() throws Exception {
        AiBusinessProcess process = process();
        when(processMapper.selectActiveById(1L, 1001L)).thenReturn(process);
        when(contextResolver.resolve(eq(1L), eq(10L), eq("order_created_update"), any()))
                .thenReturn(validationContext("order_created_update"));
        BusinessProcessSchemaDTO dto = new BusinessProcessSchemaDTO();
        dto.setExpectedSchemaHash(process.getDraftSchemaHash());
        dto.setBusinessProcessJson(objectMapper.readTree(
                validSchema("order_created_update")
                        .replace("\"objectId\":\"20\"", "\"objectId\":\"21\"")
                        .replace("\"objectCode\":\"order\"", "\"objectCode\":\"outside_order\"")));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveSchema(1001L, dto));

        assertEquals(422, error.getCode());
        verify(processMapper, never()).updateDraftSchema(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("copy keeps only the draft and rebuilds every node and edge id")
    void copyRebuildsGraphIdsAndDropsPublishedState() {
        AiBusinessProcess source = process();
        source.setDraftSchemaJson(validSchema("order_created_update"));
        source.setPublishedVersion(3);
        source.setCurrentVersion(3);
        source.setLegacySourceType("TRIGGER");
        source.setLegacySourceId("88");
        when(processMapper.selectActiveById(1L, 1001L)).thenReturn(source);
        when(processMapper.selectActiveByCode(1L, 10L, "order_created_update_copy")).thenReturn(null);
        when(contextResolver.resolve(eq(1L), eq(10L), eq("order_created_update_copy"), any()))
                .thenReturn(validationContext("order_created_update_copy"));
        when(processMapper.insert(any(AiBusinessProcess.class))).thenAnswer(invocation -> {
            AiBusinessProcess process = invocation.getArgument(0);
            process.setId(1002L);
            return 1;
        });
        BusinessProcessDTO dto = new BusinessProcessDTO();
        dto.setProcessCode("order_created_update_copy");
        dto.setProcessName("订单审批流程副本");

        BusinessProcessVO result = service.copy(1001L, dto);

        ArgumentCaptor<AiBusinessProcess> captor = ArgumentCaptor.forClass(AiBusinessProcess.class);
        verify(processMapper).insert(captor.capture());
        AiBusinessProcess copied = captor.getValue();
        BusinessProcessSchema sourceSchema = validator.normalize(source.getDraftSchemaJson());
        BusinessProcessSchema copiedSchema = validator.normalize(copied.getDraftSchemaJson());
        Set<String> sourceNodeIds = sourceSchema.getNodes().stream().map(node -> node.getId()).collect(java.util.stream.Collectors.toSet());
        Set<String> copiedNodeIds = copiedSchema.getNodes().stream().map(node -> node.getId()).collect(java.util.stream.Collectors.toSet());
        assertEquals("1002", result.getId());
        assertEquals("order_created_update_copy", copiedSchema.getProcessCode());
        assertTrue(java.util.Collections.disjoint(sourceNodeIds, copiedNodeIds));
        assertTrue(copiedSchema.getEdges().stream().allMatch(edge -> copiedNodeIds.contains(edge.getSource())
                && copiedNodeIds.contains(edge.getTarget())));
        assertNotEquals(sourceSchema.getEdges().get(0).getId(), copiedSchema.getEdges().get(0).getId());
        assertEquals(0, copied.getCurrentVersion());
        assertNull(copied.getPublishedVersion());
        assertNull(copied.getLegacySourceType());
        assertNull(copied.getLegacySourceId());
    }

    @Test
    @DisplayName("designer read repairs an unbound approval node to the unique application page form")
    void designerReadRepairsLegacyApprovalFormBinding() throws Exception {
        AiBusinessProcess process = process();
        process.setDraftSchemaJson(approvalSchema("order_created_update"));
        process.setDraftSchemaHash(validator.schemaHash(validator.normalize(process.getDraftSchemaJson())));
        when(processMapper.selectActiveById(1L, 1001L)).thenReturn(process);
        when(contextResolver.resolve(eq(1L), eq(10L), eq("order_created_update"), any()))
                .thenReturn(validationContext("order_created_update"));
        when(businessFlowService.getFormAssets("order", true, 10L)).thenReturn(Map.of(
                "formAssets", List.of(Map.of(
                        "formKey", "app_10_page_page_page_form_order_form",
                        "formName", "订单页面",
                        "formMode", "BUSINESS_OBJECT_FORM",
                        "applicationId", "10",
                        "pageId", "page_order",
                        "pageName", "订单"))));
        BusinessProcessVO result = service.getDesigner(1001L);

        Map<String, Object> config = result.getBusinessProcessJson().getNodes().stream()
                .filter(node -> "APPROVAL".equals(node.getType()))
                .findFirst().orElseThrow().getConfig();
        assertEquals("app_10_page_page_page_form_order_form",
                ((Map<?, ?>) config.get("formAsset")).get("formKey"));
        assertEquals("page_order", ((Map<?, ?>) config.get("formAsset")).get("pageId"));
        assertTrue(result.getBusinessProcessJson().getDependencies().getFormAssets()
                .contains("app_10_page_page_page_form_order_form"));
        verify(processMapper, never()).updateDraftSchema(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("an active run record blocks logical deletion")
    void activeRunRecordBlocksDeletion() {
        when(processMapper.selectActiveById(1L, 1001L)).thenReturn(process());
        when(runMapper.countByProcessId(1L, 1001L)).thenReturn(1L);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.logicalDelete(1001L));

        assertTrue(error.getMessage().contains("活跃运行记录"));
        verify(processMapper, never()).logicalDelete(any(), any(), any());
    }

    @Test
    @DisplayName("terminal run records (CANCELED/SUCCESS/FAILED) do not block logical deletion")
    void terminalRunRecordsDoNotBlockDeletion() {
        when(processMapper.selectActiveById(1L, 1001L)).thenReturn(process());
        // SQL only counts PENDING/RUNNING/WAITING; terminal records are excluded
        when(runMapper.countByProcessId(1L, 1001L)).thenReturn(0L);
        when(processMapper.logicalDelete(eq(1L), eq(1001L), any())).thenReturn(1);

        service.logicalDelete(1001L);

        verify(processMapper).logicalDelete(eq(1L), eq(1001L), any());
    }

    @Test
    @DisplayName("published versions are retained as history and do not block logical deletion")
    void publishedVersionsDoNotBlockDeletion() {
        when(processMapper.selectActiveById(1L, 1001L)).thenReturn(process());
        when(runMapper.countByProcessId(1L, 1001L)).thenReturn(0L);
        when(processMapper.logicalDelete(eq(1L), eq(1001L), any())).thenReturn(1);

        service.logicalDelete(1001L);

        verify(processMapper).logicalDelete(eq(1L), eq(1001L), any());
    }

    private AiBusinessApplication application() {
        AiBusinessApplication application = new AiBusinessApplication();
        application.setId(10L);
        application.setTenantId(1L);
        application.setApplicationCode("order_center");
        application.setStatus(1);
        return application;
    }

    private BusinessApplicationObjectVO applicationObject() {
        BusinessApplicationObjectVO object = new BusinessApplicationObjectVO();
        object.setObjectId(20L);
        object.setObjectCode("order");
        object.setObjectName("订单");
        object.setObjectStatus(1);
        return object;
    }

    private AiBusinessProcess process() {
        AiBusinessProcess process = new AiBusinessProcess();
        process.setId(1001L);
        process.setTenantId(1L);
        process.setApplicationId(10L);
        process.setProcessCode("order_created_update");
        process.setProcessName("订单审批流程");
        process.setSubjectObjectId(20L);
        process.setSubjectObjectCode("order");
        process.setDraftSchemaJson(validSchema("order_created_update"));
        process.setDraftSchemaHash(validator.schemaHash(validator.normalize(process.getDraftSchemaJson())));
        process.setDesignStatus("DRAFT");
        process.setCurrentVersion(0);
        process.setStatus(1);
        return process;
    }

    private BusinessProcessValidationContext validationContext(String processCode) {
        return new BusinessProcessValidationContext()
                .setExpectedProcessCode(processCode)
                .setObjectIdsByCode(Map.of("order", "20"))
                .setPublishedObjectVersionIdsByCode(Map.of("order", "120"))
                .setFieldsByObjectCode(Map.of("order", Set.of("id", "status")))
                .setKnownPermissions(Set.of("ai:businessProcess:start"));
    }

    private String validSchema(String processCode) {
        return """
                {
                  "schemaVersion":"1.0",
                  "processCode":"%s",
                  "subject":{"objectId":"20","objectCode":"order","objectVersionId":null,"recordIdSource":"EVENT_RECORD"},
                  "nodes":[
                    {"id":"start","type":"START_EVENT","name":"订单新增","config":{"eventType":"RECORD_CREATED"}},
                    {"id":"update","type":"ACTION","name":"更新状态","config":{"actionType":"UPDATE_RECORD","objectCode":"order","fieldMappings":[{"field":"status","valueSource":"CONSTANT","value":"OPEN"}]}},
                    {"id":"end","type":"END","name":"完成","config":{"result":"SUCCESS"}}
                  ],
                  "edges":[
                    {"id":"e1","source":"start","target":"update","sourcePort":"NEXT"},
                    {"id":"e2","source":"update","target":"end","sourcePort":"NEXT"}
                  ],
                  "policies":{"approvalConcurrency":"ONE_ACTIVE_PER_BUSINESS_KEY","maxSubProcessDepth":5,"retry":{"mode":"LIMITED","maxAttempts":3,"backoffSeconds":[30,120,600]}},
                  "dependencies":{"objects":["order"],"flowModels":[],"formAssets":[],"businessActions":[],"messageTemplates":[],"capabilities":[],"subProcesses":[]}
                }
                """.formatted(processCode);
    }

    private String approvalSchema(String processCode) {
        return """
                {
                  "schemaVersion":"1.0",
                  "processCode":"%s",
                  "subject":{"objectId":"20","objectCode":"order","objectVersionId":"120","recordIdSource":"RUNTIME_RECORD"},
                  "nodes":[
                    {"id":"start","type":"START_MANUAL","name":"发起","config":{}},
                    {"id":"approval","type":"APPROVAL","name":"审批","ports":["APPROVED","REJECTED","CANCELED","FAILED"],"config":{"flowModelKey":"order_approval","versionPolicy":"PINNED_AT_APPLICATION_PUBLISH","statusField":"flowStatus","formAsset":{}}},
                    {"id":"end","type":"END","name":"完成","config":{"result":"SUCCESS"}}
                  ],
                  "edges":[
                    {"id":"e1","source":"start","target":"approval","sourcePort":"NEXT"},
                    {"id":"e2","source":"approval","target":"end","sourcePort":"APPROVED"},
                    {"id":"e3","source":"approval","target":"end","sourcePort":"REJECTED"},
                    {"id":"e4","source":"approval","target":"end","sourcePort":"CANCELED"},
                    {"id":"e5","source":"approval","target":"end","sourcePort":"FAILED"}
                  ],
                  "policies":{"approvalConcurrency":"ONE_ACTIVE_PER_BUSINESS_KEY","maxSubProcessDepth":5,"retry":{"mode":"LIMITED","maxAttempts":3,"backoffSeconds":[30,120,600]}},
                  "dependencies":{"objects":["order"],"flowModels":["order_approval"],"formAssets":[],"businessActions":[],"messageTemplates":[],"capabilities":[],"subProcesses":[]}
                }
                """.formatted(processCode);
    }

    private String incompleteSchema() {
        return """
                {
                  "schemaVersion":"1.0",
                  "processCode":"order_created_update",
                  "subject":{"objectId":"20","objectCode":"order","objectVersionId":null,"recordIdSource":"EVENT_RECORD"},
                  "nodes":[
                    {"id":"start","type":"START_EVENT","name":"订单新增","config":{"eventType":"RECORD_CREATED"}}
                  ],
                  "edges":[],
                  "policies":{"approvalConcurrency":"ONE_ACTIVE_PER_BUSINESS_KEY","maxSubProcessDepth":5,"retry":{"mode":"LIMITED","maxAttempts":3,"backoffSeconds":[30,120,600]}},
                  "dependencies":{"objects":["order"],"flowModels":[],"formAssets":[],"businessActions":[],"messageTemplates":[],"capabilities":[],"subProcesses":[]}
                }
                """;
    }
}
