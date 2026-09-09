package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowTaskMutationAuthorizationContractTest {

    @Test
    void highImpactTaskActionsMustCheckMutationActorBeforeFlowableSideEffects() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java"));
        int delegateStart = source.indexOf("public void delegate(String taskId");
        int terminateStart = source.indexOf("public void terminateTask(String taskId");
        assertTrue(delegateStart >= 0 && terminateStart > delegateStart);
        assertTrue(source.substring(delegateStart, terminateStart).contains("assertTaskMutationActor"));
        assertTrue(source.substring(delegateStart, terminateStart).contains("validateReassignTarget(targetUserId.trim())"));
        int terminateEnd = source.indexOf("private Map<String, Object> mergeActionVariables", terminateStart);
        assertTrue(source.substring(terminateStart, terminateEnd).contains("assertTaskMutationActor"));
    }

    @Test
    void visibleHistoryAndTaskFormMustReuseTenantBoundBusinessLookup() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java"));
        int historyStart = source.indexOf("public List<Map<String, Object>> getProcessHistory");
        int formStart = source.indexOf("public TaskFormInfo getTaskFormInfo");
        assertTrue(historyStart >= 0 && formStart >= 0 && formStart < historyStart);
        String history = source.substring(historyStart);
        assertTrue(history.contains("flowAccessGuard.requireProcessVisible"));
        assertTrue(history.contains("FlowBusiness business = flowAccessGuard.requireProcessVisible"));
        assertTrue(history.contains("selectByProcessInstanceIdAndTenantId") || !history.contains("selectByProcessInstanceId(processInstanceId)"));
        String form = source.substring(formStart, historyStart);
        assertTrue(form.contains("flowAccessGuard.requireTaskVisible"));
        assertTrue(form.contains("selectByProcessInstanceIdAndTenantId"));
    }

    @Test
    void processDetailsMustCapHistoricalCollections() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java"));
        assertTrue(source.contains("MAX_DETAIL_HISTORY_ITEMS = 1000"));
        assertTrue(source.contains("listPage(0, MAX_DETAIL_HISTORY_ITEMS)"));
        assertTrue(source.contains("selectHistoryTasks"));
        assertTrue(source.contains("Math.min(pageSize, MAX_DETAIL_HISTORY_ITEMS)"));
    }

    @Test
    void taskMutationLocksMustBeTenantScoped() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java"));
        assertTrue(source.contains("selectByTaskIdForUpdateAndTenant(taskId, tenantId)"));
        assertTrue(!source.contains("selectByTaskIdForUpdate(taskId)"),
                "flow task writes must not lock a task without the trusted tenant predicate");
    }

    @Test
    void ignoreTenantFormAndBusinessLookupsMustCarryTenantPredicates() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java"));
        assertTrue(source.contains("selectByProcessInstanceIdAndTenantIdForUpdate(\n                    task.getProcessInstanceId(), tenantId)"));
        assertTrue(source.contains("selectByProcessInstanceIdAndTenantId(processInstanceId, tenantId)"));
        assertTrue(source.contains("selectByBusinessKeyAndTenantId(tenantId, businessKey)"));
        assertTrue(source.contains("selectByIdOrTaskIdAndTenant(taskId, tenantId)"));
        assertTrue(!source.contains("flowBusinessMapper.selectByProcessInstanceId(task.getProcessInstanceId())"));
        assertTrue(!source.contains("flowBusinessMapper.selectByProcessInstanceId(processInstanceId)"));
        assertTrue(!source.contains("flowBusinessMapper.selectByBusinessKey(businessKey)"));
        assertTrue(source.contains("selectByProcessInstanceIdAndTenantId(\n                    processInstanceId, tenantId)"));
    }

    @Test
    void formInstanceReadMapperMustExposeTenantBoundVariants() throws IOException {
        String mapper = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/mapper/FlowFormInstanceMapper.java"));
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/FlowFormInstanceMapper.xml"));
        assertTrue(mapper.contains("selectByProcessInstanceIdAndTenantId"));
        assertTrue(mapper.contains("selectByBusinessKeyAndTenantId"));
        assertTrue(xml.contains("id=\"selectByProcessInstanceIdAndTenantId\""));
        assertTrue(xml.contains("id=\"selectByBusinessKeyAndTenantId\""));
        assertTrue(xml.contains("AND i.tenant_id = #{tenantId}"));
    }

    @Test
    void runtimeAndCcReadbacksMustRemainTenantBound() throws IOException {
        String runtimeService = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowRuntimeServiceImpl.java"));
        String monitorService = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowMonitorServiceImpl.java"));
        String ccService = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowCcServiceImpl.java"));
        assertTrue(runtimeService.contains("requireTenantIdForRead()"));
        assertTrue(runtimeService.contains("selectByProcessInstanceIdAndTenantId"));
        assertTrue(monitorService.contains("return requireCurrentTenantProcessInstance(processInstanceId)"));
        assertTrue(!monitorService.contains("flowBusinessMapper.selectByProcessInstanceId(processInstanceId)"));
        assertTrue(ccService.contains("selectByProcessInstanceIdAndTenantId(processInstanceId, tenantId)"));
        assertTrue(!ccService.contains("flowBusinessMapper.selectByProcessInstanceId(processInstanceId)"));
    }

    @Test
    void processHistoryMustUseTenantMapperAndBatchUserLookup() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java"));
        int start = source.indexOf("public List<Map<String, Object>> getProcessHistory");
        int end = source.indexOf("private String extractProcessKey", start);
        assertTrue(start >= 0 && end > start);
        String method = source.substring(start, end);
        assertTrue(method.contains("selectHistoryTasks"));
        assertTrue(method.contains("requireTenant()"));
        assertTrue(method.contains("getUserInfoBatch"));
        assertTrue(!method.contains("LambdaQueryWrapper"));
    }

    @Test
    void accessGuardMustReadCandidateRelationsWithLegacyFallback() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/security/FlowAccessGuard.java"));
        assertTrue(source.contains("FlowTaskCandidateMapper"));
        assertTrue(source.contains("countActiveByTaskAndValue"));
        assertTrue(source.contains("containsCsv(task.getCandidateUsers(), userId)"));
        assertTrue(source.contains("hasCandidateGroup(task)"));
        assertTrue(source.contains("task.getTenantId()"));
    }

    @Test
    void historyPageMustExposeTypedBoundedProtocol() throws IOException {
        String service = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java"));
        String controller = Files.readString(Path.of(
                "../../../forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowTaskController.java"));
        assertTrue(service.contains("FlowHistoryPageVO getProcessHistoryPage"));
        assertTrue(service.contains("Math.min(pageSize, MAX_DETAIL_HISTORY_ITEMS)"));
        assertTrue(service.contains("selectHistoryTasks"));
        assertTrue(service.contains("taskPage.getCurrent() * taskPage.getSize() < taskPage.getTotal()"));
        assertTrue(controller.contains("/history/{processInstanceId}/page"));
        assertTrue(controller.contains("FlowHistoryPageVO"));
    }

    @Test
    void processRollbackMustValidateRuntimeAndTargetNodeBeforeChangingState() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowInstanceServiceImpl.java"));
        int start = source.indexOf("public void rollbackToActivity");
        int end = source.indexOf("public void reassignTask", start);
        assertTrue(start >= 0 && end > start);
        String method = source.substring(start, end);
        assertTrue(method.contains("processInstanceId(processInstanceId).singleResult()"));
        assertTrue(method.contains("getCurrentActivityIds(processInstanceId)"));
        assertTrue(method.contains("getFlowElement(targetActivityId)"));
        assertTrue(method.contains("target instanceof FlowNode"));
        assertTrue(method.contains("currentActivityIds.contains(targetActivityId)"));
    }

    @Test
    void adminReassignMustReloadLocalTaskWithTenantCondition() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowInstanceServiceImpl.java"));
        int start = source.indexOf("public void reassignTask");
        int end = source.indexOf("public void terminateProcessByInstanceId", start);
        assertTrue(start >= 0 && end > start);
        String method = source.substring(start, end);
        assertTrue(method.contains("selectByTaskIdAndTenant(taskId, tenantId)"));
        assertTrue(!method.contains("selectByTaskId(taskId)"));
        assertTrue(method.contains("FlowTaskStatus.CLAIMED.getCode()"));
    }
}
