package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.businessprocess.validation.BusinessProcessSchemaValidator;
import com.mdframe.forge.plugin.generator.businessprocess.schema.BusinessProcessSchema;
import com.mdframe.forge.plugin.generator.businessprocess.validation.BusinessProcessValidationContext;
import com.mdframe.forge.plugin.generator.businessprocess.validation.BusinessProcessValidationContextResolver;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcess;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationAssetSelectionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationReadinessVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectTableFieldMappingVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectTableMappingVO;
import com.mdframe.forge.plugin.generator.vo.businessprocess.BusinessProcessValidationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessApplicationReadinessService")
class BusinessApplicationReadinessServiceTest {

    @Mock
    private BusinessApplicationService applicationService;
    @Mock
    private BusinessApplicationAssetSelectionService selectionService;
    @Mock
    private BusinessObjectPublishService objectPublishService;
    @Mock
    private BusinessPermissionService permissionService;
    @Mock
    private BusinessBindingMapper bindingMapper;
    @Mock
    private BusinessApplicationPageDependencyInspector pageDependencyInspector;
    @Mock
    private BusinessObjectTableMappingService tableMappingService;
    @Mock
    private BusinessProcessSchemaValidator processSchemaValidator;
    @Mock
    private BusinessProcessValidationContextResolver processValidationContextResolver;

    private BusinessApplicationReadinessService service;
    private BusinessApplicationObjectVO object;

    @BeforeEach
    void setUp() {
        service = new BusinessApplicationReadinessService(
                applicationService, selectionService, objectPublishService, permissionService,
                bindingMapper, pageDependencyInspector, tableMappingService,
                processSchemaValidator, processValidationContextResolver);
        object = object();
        BusinessApplicationVO application = application();
        BusinessApplicationAssetSelectionVO selection = new BusinessApplicationAssetSelectionVO();
        selection.setObjectIds(List.of(object.getObjectId()));
        selection.setIncludeAutomation(false);
        BusinessApplicationAssetSelectionService.ResolvedSelection resolved
                = new BusinessApplicationAssetSelectionService.ResolvedSelection(
                        selection, List.of(object), List.of(), List.of(), List.of());

        when(applicationService.publishContext(101L)).thenReturn(application);
        when(applicationService.slugAvailable("leave_center", 101L)).thenReturn(true);
        when(selectionService.resolveContext(101L, null)).thenReturn(resolved);
        when(pageDependencyInspector.inspect(application, List.of(object)))
                .thenReturn(new BusinessApplicationPageDependencyInspector.InspectionResult(false, List.of()));
        when(permissionService.documentActionSummaries(anyList())).thenReturn(List.of());
        // object.designStatus=PUBLISHED：应用门禁不加载对象设计上下文
    }

    @Test
    @DisplayName("PUBLISHED object skips design context load during readiness")
    void publishedObjectSkipsDesignContextLoad() {
        object.setSyncStatus("IN_SYNC");
        object.setDesignVersion(3);
        object.setSyncDesignVersion(3);

        service.check(101L);

        verify(objectPublishService, never()).loadContextForApplicationPublish(object.getObjectId());
        verify(objectPublishService, never()).publishCheckResolved(object.getObjectId(), null);
    }

    @Test
    @DisplayName("live table mapping overrides stale object summary")
    void liveTableMappingOverridesStaleObjectSummary() {
        object.setSyncStatus("OUT_OF_SYNC");
        when(tableMappingService.getTableMapping(object.getObjectId()))
                .thenReturn(mapping("IN_SYNC", List.of(), 0));

        BusinessApplicationReadinessVO readiness = service.check(101L);

        assertFalse(readiness.getIssues().stream()
                .anyMatch(issue -> "OBJECT_DATABASE_OUT_OF_SYNC".equals(issue.getIssueCode())));
    }

    @Test
    @DisplayName("version-matched IN_SYNC summary skips live table mapping")
    void versionMatchedInSyncSummarySkipsLiveTableMapping() {
        object.setSyncStatus("IN_SYNC");
        object.setDesignVersion(7);
        object.setSyncDesignVersion(7);

        BusinessApplicationReadinessVO readiness = service.check(101L);

        assertFalse(readiness.getIssues().stream()
                .anyMatch(issue -> issue.getIssueCode() != null
                        && issue.getIssueCode().startsWith("OBJECT_DATABASE_")));
        verify(tableMappingService, never()).getTableMapping(object.getObjectId());
    }

    @Test
    @DisplayName("stale IN_SYNC after design change forces live table mapping")
    void staleInSyncAfterDesignChangeForcesLiveTableMapping() {
        object.setSyncStatus("IN_SYNC");
        object.setDesignVersion(9);
        object.setSyncDesignVersion(7);
        when(tableMappingService.getTableMapping(object.getObjectId()))
                .thenReturn(mapping("OUT_OF_SYNC", List.of(
                        field("ys_type", "预算类型", "varchar", "int", "TYPE_MISMATCH", true)), 1));

        BusinessApplicationReadinessVO readiness = service.check(101L);

        assertTrue(readiness.getIssues().stream()
                .anyMatch(issue -> "OBJECT_DATABASE_OUT_OF_SYNC".equals(issue.getIssueCode())));
        verify(tableMappingService).getTableMapping(object.getObjectId());
    }

    @Test
    @DisplayName("database blocker explains concrete live differences")
    void databaseBlockerExplainsConcreteDifferences() {
        BusinessObjectTableFieldMappingVO missing = field(
                "leave_reason", "离职原因", "varchar", null, "MISSING_DATABASE_COLUMN", true);
        BusinessObjectTableFieldMappingVO mismatch = field(
                "leave_date", "离职日期", "date", "datetime", "TYPE_MISMATCH", true);
        when(tableMappingService.getTableMapping(object.getObjectId()))
                .thenReturn(mapping("OUT_OF_SYNC", List.of(missing, mismatch), 2));

        BusinessApplicationReadinessVO readiness = service.check(101L);

        assertTrue(readiness.getIssues().stream()
                .filter(issue -> "OBJECT_DATABASE_OUT_OF_SYNC".equals(issue.getIssueCode()))
                .map(issue -> issue.getMessage())
                .anyMatch(message -> message.contains("缺少数据库列 leave_reason")
                        && message.contains("离职日期类型不一致")
                        && message.contains("设计 date，数据库 datetime")));
    }

    @Test
    @DisplayName("database blocker message excludes non-blocking retained columns")
    void databaseBlockerMessageExcludesNonBlockingRetainedColumns() {
        BusinessObjectTableFieldMappingVO retained = field(
                "department_name", "部门名称", null, "varchar(128)",
                "UNMAPPED_DATABASE_COLUMN", false);
        BusinessObjectTableFieldMappingVO blocking = field(
                "approval_code", "审批编码", null, "varchar(64)",
                "UNMAPPED_DATABASE_COLUMN", true);
        when(tableMappingService.getTableMapping(object.getObjectId()))
                .thenReturn(mapping("OUT_OF_SYNC", List.of(retained, blocking), 0));

        BusinessApplicationReadinessVO readiness = service.check(101L);

        assertTrue(readiness.getIssues().stream()
                .filter(issue -> "OBJECT_DATABASE_OUT_OF_SYNC".equals(issue.getIssueCode()))
                .map(issue -> issue.getMessage())
                .anyMatch(message -> message.contains("存在未映射业务列 approval_code")
                        && !message.contains("department_name")));
    }

    @Test
    @DisplayName("process graph and concurrency errors block application publish")
    void processValidationErrorsBlockPublish() {
        AiBusinessProcess process = new AiBusinessProcess();
        process.setId(301L);
        process.setApplicationId(101L);
        process.setProcessCode("leave_submit");
        process.setProcessName("离职审批");
        process.setDraftSchemaJson("{}");
        process.setStatus(1);
        BusinessApplicationAssetSelectionVO selection = new BusinessApplicationAssetSelectionVO();
        selection.setObjectIds(List.of(object.getObjectId()));
        selection.setProcessIds(List.of(process.getId()));
        selection.setIncludeAutomation(true);
        BusinessApplicationAssetSelectionService.ResolvedSelection resolved
                = new BusinessApplicationAssetSelectionService.ResolvedSelection(
                        selection, List.of(object), List.of(), List.of(), List.of(process));
        BusinessProcessSchema schema = new BusinessProcessSchema();
        BusinessProcessValidationContext context = new BusinessProcessValidationContext();
        BusinessProcessValidationVO validation = new BusinessProcessValidationVO();
        validation.addError("APPROVAL_CONCURRENCY_INVALID", "审批并发策略无效", null,
                "policies.approvalConcurrency", "使用单活动审批策略");
        validation.finish();

        when(selectionService.resolveContext(101L, null)).thenReturn(resolved);
        when(tableMappingService.getTableMapping(object.getObjectId()))
                .thenReturn(mapping("IN_SYNC", List.of(), 0));
        when(processSchemaValidator.normalize("{}")).thenReturn(schema);
        when(processValidationContextResolver.resolve(1L, 101L, "leave_submit", schema))
                .thenReturn(context);
        when(processSchemaValidator.validate(schema, context)).thenReturn(validation);
        when(bindingMapper.selectByApplication(1L, 101L)).thenReturn(List.of());

        BusinessApplicationReadinessVO readiness = service.check(101L);

        assertFalse(readiness.getReady());
        assertTrue(readiness.getIssues().stream()
                .anyMatch(issue -> "PROCESS_APPROVAL_CONCURRENCY_INVALID".equals(issue.getIssueCode())));
    }

    private static BusinessApplicationVO application() {
        BusinessApplicationVO application = new BusinessApplicationVO();
        application.setId(101L);
        application.setApplicationCode("leave_center");
        application.setPortalSlug("leave_center");
        application.setApplicationName("离职管理");
        application.setSuiteName("人力资源");
        application.setStatus(1);
        application.setOptions("{}");
        return application;
    }

    private static BusinessApplicationObjectVO object() {
        BusinessApplicationObjectVO object = new BusinessApplicationObjectVO();
        object.setObjectId(201L);
        object.setObjectCode("leave_application");
        object.setObjectName("离职申请");
        object.setObjectStatus(1);
        object.setTableName("biz_leave_application");
        object.setDesignStatus("PUBLISHED");
        return object;
    }

    private static BusinessObjectTableMappingVO mapping(
            String status, List<BusinessObjectTableFieldMappingVO> fields, int pendingDdlCount) {
        BusinessObjectTableMappingVO mapping = new BusinessObjectTableMappingVO();
        mapping.setTableName("biz_leave_application");
        mapping.setTableExists(true);
        mapping.setSyncStatus(status);
        mapping.setFields(fields);
        mapping.setPendingDdlCount(pendingDdlCount);
        return mapping;
    }

    private static BusinessObjectTableFieldMappingVO field(
            String columnName, String businessName, String dataType,
            String databaseType, String syncStatus, boolean blockingDifference) {
        BusinessObjectTableFieldMappingVO field = new BusinessObjectTableFieldMappingVO();
        field.setColumnName(columnName);
        field.setBusinessName(businessName);
        field.setDataType(dataType);
        field.setDatabaseType(databaseType);
        field.setSyncStatus(syncStatus);
        field.setBlockingDifference(blockingDifference);
        return field;
    }
}
