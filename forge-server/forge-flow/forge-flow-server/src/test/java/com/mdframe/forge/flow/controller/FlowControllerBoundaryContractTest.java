package com.mdframe.forge.flow.controller;

import com.mdframe.forge.starter.core.util.PageParamResolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlowControllerBoundaryContractTest {

    private static final Path FLOW_MAPPER_DIR = Path.of(
            "../../forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper");
    private static final Path SYSTEM_CONTROLLER_DIR = Path.of(
            "../../forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller");
    private static final Path FLOW_CLEANUP_PERMISSION_MIGRATION = Path.of(
            "../../db/migration/V1.0.55__secure_flow_monitor_cleanup.sql");

    @Test
    void controllersShouldNotUseMybatisWrappersOrMappersDirectly() throws IOException {
        for (String controller : List.of(
                "FlowInstanceController.java",
                "FlowErrorLogController.java",
                "FlowMonitorController.java")) {
            String source = Files.readString(Path.of("src/main/java/com/mdframe/forge/flow/controller", controller));

            assertThat(source)
                    .as(controller)
                    .doesNotContain("LambdaQueryWrapper", ".mapper.", "Mapper ");
        }
    }

    @Test
    void cleanupSqlShouldKeepDeclaredLogicalAndPhysicalDeleteSemantics() throws IOException {
        assertPhysicalDelete("FlowTaskMapper.xml");
        assertPhysicalDelete("FlowCommentMapper.xml");
        assertPhysicalDelete("FlowCcMapper.xml");
        assertPhysicalDelete("FlowErrorLogMapper.xml");
        assertPhysicalDelete("FlowBusinessMapper.xml");

        assertLogicalDelete("FlowFormInstanceMapper.xml");
        assertLogicalDelete("FlowFillBatchItemMapper.xml");
    }

    @Test
    void flowBusinessQueriesShouldBeDeclaredInMapperXml() throws IOException {
        String xml = mapperXml("FlowBusinessMapper.xml");

        assertThat(xml).contains(
                "id=\"selectBusinessPage\"",
                "id=\"selectBusinessesForCleanup\"",
                "id=\"selectDailyTrend\"",
                "id=\"selectProcessDistribution\"",
                "id=\"updateStatusByProcessInstanceId\"");
    }

    @Test
    void cleanupEndpointShouldKeepConfirmationGateWithoutPromisingTransactionRollback() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/flow/controller/FlowMonitorController.java"));

        assertThat(source)
                .contains("\"确认删除流程数据\"", "flowMonitorService.cleanupProcessInstances(")
                .doesNotContain("@Transactional");
    }

    @Test
    void cleanupEndpointsShouldRequireExplicitPermissionAndTenantIsolation() throws IOException {
        String controller = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/flow/controller/FlowMonitorController.java"));
        String migration = Files.readString(FLOW_CLEANUP_PERMISSION_MIGRATION);

        assertThat(controller)
                .contains("@SaCheckPermission(\"flow:monitor:cleanup\")\n    @PostMapping(\"/instance/",
                        "@SaCheckPermission(\"flow:monitor:cleanup\")\n    @PostMapping(\"/instances/cleanup\")")
                .doesNotContain("@IgnoreTenant");
        assertThat(migration)
                .contains("'flow:monitor:cleanup'", "'/api/flow/monitor/instances/cleanup'",
                        "'/api/flow/monitor/instance/*/delete'", "tenant_id = 1", "NOT EXISTS",
                        "'flow:monitor:cleanup:api:batch'", "'flow:monitor:cleanup:api:single'",
                        "管理和清理 API 不做任何角色回填")
                .doesNotContain("1, 1, 1, 'flow:monitor:cleanup', NULL,\n       'POST'");
    }

    @Test
    void monitorEndpointsShouldSeparateViewManageAndCleanupPermissions() throws IOException {
        String controller = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/flow/controller/FlowMonitorController.java"));
        String migration = Files.readString(FLOW_CLEANUP_PERMISSION_MIGRATION);

        assertThat(controller)
                .contains("@SaCheckPermission(\"flow:monitor:view\")",
                        "@SaCheckPermission(\"flow:monitor:manage\")",
                        "@SaCheckPermission(\"flow:monitor:cleanup\")",
                        "flowMonitorService.assertCurrentTenantProcessInstance(processInstanceId)");
        assertThat(migration).contains(
                "SET perms = 'flow:monitor:view'",
                "'flow:monitor:manage' perms",
                "'flow:monitor:view:api:read'",
                "'flow:monitor:manage:api:write'",
                "'flow:monitor:manage:api:update'",
                "'PUT', '/api/flow/monitor/error-logs/*/resolve'");
    }

    @Test
    void errorLogEndpointsAndSqlShouldEnforceTenantAndPermissionBoundaries() throws IOException {
        String controller = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/flow/controller/FlowErrorLogController.java"));
        String mapper = mapperXml("FlowErrorLogMapper.xml");

        assertThat(controller)
                .contains("@SaCheckPermission(\"flow:monitor:view\")",
                        "@SaCheckPermission(\"flow:monitor:manage\")",
                        "flowErrorLogService.getCurrentTenantError(logId)",
                        "flowErrorLogService.resolveError(logId, userId)")
                .doesNotContain("@IgnoreTenant", "flowErrorLogService.getById(logId)");
        assertThat(mapper)
                .contains("id=\"selectByIdAndTenantId\"", "id=\"selectByIdAndTenantIdForUpdate\"",
                        "id=\"resolveByIdAndTenantId\"", "id=\"updateRetryState\"");
        assertThat(statement(mapper, "update", "resolveByIdAndTenantId"))
                .contains("tenant_id = #{tenantId}");
        assertThat(statement(mapper, "update", "updateRetryState"))
                .contains("tenant_id = #{tenantId}");
    }

    @Test
    void monitorQueriesAndManagementTransactionsShouldLockCurrentTenantRows() throws IOException {
        String businessMapper = mapperXml("FlowBusinessMapper.xml");
        String monitorService = Files.readString(Path.of(
                "../../forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/"
                        + "com/mdframe/forge/starter/flow/service/impl/FlowMonitorServiceImpl.java"));
        String instanceService = Files.readString(Path.of(
                "../../forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/"
                        + "com/mdframe/forge/starter/flow/service/impl/FlowInstanceServiceImpl.java"));

        assertThat(businessMapper).contains(
                "id=\"selectMonitorStatistics\"", "id=\"selectByProcessInstanceIdAndTenantIdForUpdate\"",
                "business.tenant_id = #{tenantId}", "task.tenant_id = #{tenantId}",
                "title_source.tenant_id = #{tenantId}");
        assertThat(monitorService).contains(
                "selectMonitorStatistics(tenantId, startOfDay)",
                "selectDailyTrend(",
                "tenantId, firstDay.atStartOfDay()",
                "selectProcessDistribution(tenantId)",
                "@Transactional(rollbackFor = Exception.class)\n    public void suspendProcessInstance",
                "@Transactional(rollbackFor = Exception.class)\n    public void activateProcessInstance",
                "lockCurrentTenantProcessInstance(processInstanceId, tenantId)");
        assertThat(instanceService).contains(
                "lockCurrentTenantProcessInstance(processInstanceId)",
                "lockCurrentTenantProcessInstance(task.getProcessInstanceId())",
                "selectByProcessInstanceIdAndTenantIdForUpdate");
    }

    @Test
    void permissionMigrationShouldAvoidCompatibilityUpdatesThatCanCollide() throws IOException {
        String migration = Files.readString(FLOW_CLEANUP_PERMISSION_MIGRATION);

        assertThat(migration)
                .doesNotContain("CASE api_url", "兼容脚本曾被人工部分执行");
    }

    @Test
    void errorLogManagementShouldLockBusinessBeforeErrorLog() throws IOException {
        String service = Files.readString(Path.of(
                "../../forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/"
                        + "com/mdframe/forge/starter/flow/service/impl/FlowErrorLogServiceImpl.java"));

        int retryMethod = service.indexOf("public void retryNode(");
        int retryBusinessLock = service.indexOf("selectByProcessInstanceIdAndTenantIdForUpdate", retryMethod);
        int retryErrorLock = service.indexOf("findRetryLog(", retryBusinessLock);
        assertThat(retryMethod).isGreaterThanOrEqualTo(0);
        assertThat(retryBusinessLock).isGreaterThan(retryMethod).isLessThan(retryErrorLock);

        int resolveMethod = service.indexOf("public void resolveError(");
        int resolveBusinessLock = service.indexOf("selectByProcessInstanceIdAndTenantIdForUpdate", resolveMethod);
        int resolveErrorLock = service.indexOf("selectByIdAndTenantIdForUpdate", resolveBusinessLock);
        assertThat(resolveMethod).isGreaterThanOrEqualTo(0);
        assertThat(resolveBusinessLock).isGreaterThan(resolveMethod).isLessThan(resolveErrorLock);
    }

    @Test
    void formInstanceStatusAndCleanupSqlShouldBindTenantInTheirOwnStatements() throws IOException {
        String xml = mapperXml("FlowFormInstanceMapper.xml");

        assertThat(statement(xml, "update", "updateStatusByProcessInstanceId"))
                .contains("tenant_id = #{tenantId}");
        assertThat(statement(xml, "update", "deleteByProcessInstanceIdLogically"))
                .contains("tenant_id = #{tenantId}");

        String mapper = Files.readString(Path.of(
                "../../forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/"
                        + "com/mdframe/forge/starter/flow/mapper/FlowFormInstanceMapper.java"));
        String listener = Files.readString(Path.of(
                "../../forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/"
                        + "com/mdframe/forge/starter/flow/listener/FlowTaskEventListener.java"));
        assertThat(mapper).contains("@Param(\"tenantId\") Long tenantId");
        assertThat(listener).contains(
                "updateStatusByProcessInstanceId(processInstanceId, status, tenantId)");
    }

    @Test
    void cleanupShouldUsePerInstanceTransactionsAndStablePublicFailures() throws IOException {
        String service = Files.readString(Path.of(
                "../../forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/"
                        + "com/mdframe/forge/starter/flow/service/impl/FlowMonitorServiceImpl.java"));
        String transactionExecutor = Files.readString(Path.of(
                "../../forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/"
                        + "com/mdframe/forge/starter/flow/service/support/FlowCleanupTransactionExecutor.java"));
        String controller = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/flow/controller/FlowMonitorController.java"));

        assertThat(service)
                .contains("cleanupTransactionExecutor.execute", "PROCESS_CLEANUP_FAILURE_MESSAGE",
                        "查询流程监控当前任务失败")
                .doesNotContain("failure.put(\"message\", e.getMessage())", "catch (Exception ignored)");
        assertThat(transactionExecutor)
                .contains("PROPAGATION_REQUIRES_NEW", "transactionTemplate.execute");
        assertThat(controller).doesNotContain("+ e.getMessage()");
    }

    @Test
    void cleanupCandidateQueriesShouldKeepAStablePublicErrorBoundary() throws IOException {
        String service = Files.readString(Path.of(
                "../../forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/"
                        + "com/mdframe/forge/starter/flow/service/impl/FlowMonitorServiceImpl.java"));

        assertThat(service)
                .contains("查询批量流程清理候选数据失败", "PROCESS_CLEANUP_FAILURE_MESSAGE, e")
                .doesNotContain("PROCESS_CLEANUP_FAILURE_MESSAGE + e.getMessage()");
    }

    @Test
    void cleanupShouldStayWithinCurrentTenantAndFilteredBusinessCandidates() throws IOException {
        String service = Files.readString(Path.of(
                "../../forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/"
                        + "com/mdframe/forge/starter/flow/service/impl/FlowMonitorServiceImpl.java"));
        String businessMapper = mapperXml("FlowBusinessMapper.xml");

        assertThat(service)
                .contains("resolveCleanupTenantId()", "selectBusinessesForCleanup(tenantId,",
                        "cleanupProcessInstanceIds(processInstanceIds, tenantId, reason)")
                .doesNotContain("addFlowableProcessIdsByModelKey");
        assertThat(businessMapper)
                .contains("id=\"selectByProcessInstanceIdAndTenantId\"", "tenant_id = #{tenantId}",
                        "id=\"deleteBusinessRecordsWithoutProcessInstance\"");
    }

    @Test
    void monitorServiceShouldUseOptionalUserLookupBridge() throws IOException {
        String service = Files.readString(Path.of(
                "../../forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/"
                        + "com/mdframe/forge/starter/flow/service/impl/FlowMonitorServiceImpl.java"));

        assertThat(service)
                .contains("FlowMonitorUserLookup", "userLookupProvider.getIfAvailable()")
                .doesNotContain("com.mdframe.forge.plugin.system", "ISysUserService");
    }

    @Test
    void errorLogStatisticsShouldUseMapperXmlAggregation() throws IOException {
        assertThat(mapperXml("FlowErrorLogMapper.xml"))
                .contains("<select id=\"selectStatistics\"", "AS unresolved", "AS retried", "AS retryFailed");
    }

    @Test
    void paginationShouldPreferLegacyPageAliasAndSupportStandardPageNum() throws IOException {
        assertThat(PageParamResolver.resolve(null, 4)).isEqualTo(4);
        assertThat(PageParamResolver.resolve(3, null)).isEqualTo(3);
        assertThat(PageParamResolver.resolve(3, 4)).isEqualTo(3);
        assertThat(PageParamResolver.resolve(null, null)).isEqualTo(1);

        for (Path controller : List.of(
                Path.of("src/main/java/com/mdframe/forge/flow/controller/FlowMonitorController.java"),
                Path.of("src/main/java/com/mdframe/forge/flow/controller/FlowErrorLogController.java"),
                SYSTEM_CONTROLLER_DIR.resolve("SysCacheController.java"))) {
            assertThat(Files.readString(controller))
                    .as(controller.toString())
                    .contains("PageParamResolver.resolve(page, pageNum)");
        }
    }

    @Test
    void processDistributionShouldKeepFirstNonEmptyTitleByRecordOrder() throws IOException {
        assertThat(mapperXml("FlowBusinessMapper.xml"))
                .contains("SELECT title_source.title", "ORDER BY title_source.id ASC", "LIMIT 1")
                .doesNotContain("MAX(NULLIF(title, ''))");
    }

    private void assertPhysicalDelete(String mapperFile) throws IOException {
        assertThat(statement(mapperXml(mapperFile), "delete", "deleteByProcessInstanceIdPhysically"))
                .as(mapperFile)
                .contains("DELETE FROM", "process_instance_id = #{processInstanceId}",
                        "tenant_id = #{tenantId}");
    }

    private void assertLogicalDelete(String mapperFile) throws IOException {
        assertThat(statement(mapperXml(mapperFile), "update", "deleteByProcessInstanceIdLogically"))
                .as(mapperFile)
                .contains("SET deleted = 1", "process_instance_id = #{processInstanceId}",
                        "tenant_id = #{tenantId}", "AND deleted = 0");
    }

    private String statement(String xml, String element, String id) {
        String opening = "<" + element + " id=\"" + id + "\">";
        int start = xml.indexOf(opening);
        int end = start < 0 ? -1 : xml.indexOf("</" + element + ">", start);
        assertThat(start).as(id + " start").isGreaterThanOrEqualTo(0);
        assertThat(end).as(id + " end").isGreaterThan(start);
        return xml.substring(start, end);
    }

    private String mapperXml(String mapperFile) throws IOException {
        return Files.readString(FLOW_MAPPER_DIR.resolve(mapperFile));
    }
}
