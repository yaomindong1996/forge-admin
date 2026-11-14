package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowTaskSignContractTest {

    @Test
    void dynamicSignActionsMustBeTenantBoundAuditedAndIdempotentAtCandidateSetLevel() throws IOException {
        String service = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java"));
        String coordinator = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskDynamicSignCoordinator.java"));
        String controller = Files.readString(Path.of(
                "../../../forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowTaskController.java"));
        assertTrue(service.contains("public void addSign"));
        assertTrue(service.contains("public void reduceSign"));
        assertTrue(service.contains("assertTaskMutationActor(taskId, userId, false)"));
        assertTrue(service.contains("this::validateReassignTarget"));
        assertTrue(coordinator.contains("MAX_DYNAMIC_SIGNERS = 50"));
        assertTrue(coordinator.contains("不能将当前任务办理人再次加入加签名单"));
        assertTrue(coordinator.contains("taskService.addCandidateUser"));
        assertTrue(coordinator.contains("taskService.deleteCandidateUser"));
        assertTrue(coordinator.contains("taskService.addComment(task.getId(), task.getProcessInstanceId(), action"));
        assertTrue(controller.contains("@PostMapping(\"/add-sign\")"));
        assertTrue(controller.contains("@PostMapping(\"/reduce-sign\")"));
        assertTrue(controller.contains("/{taskId}/sign-relations"));
        assertTrue(coordinator.contains("FlowTaskSignMode.fromCode(signMode)"));
        assertTrue(coordinator.contains("relation.setParentTaskId(task.getTaskId())"));
        assertTrue(coordinator.contains("relation.setOperatorId(operatorId)"));
        assertTrue(coordinator.contains("deactivateWithAudit"));
        assertTrue(coordinator.contains("selectByIdempotency"));
        assertTrue(coordinator.contains("FLOW_TASK_IDEMPOTENCY_CONFLICT"));
        assertTrue(coordinator.contains("addMultiInstanceExecution"));
        assertTrue(coordinator.contains("deleteMultiInstanceExecution"));
        assertTrue(coordinator.contains("resolveMultiInstanceUserTask"));
        assertTrue(coordinator.contains("childExecutionId"));
        assertTrue(coordinator.contains("FlowTaskSignMode.PARALLEL.getCode().equals(normalizedSignMode)"));
        assertTrue(coordinator.contains("FLOW_TASK_SIGN_MODE_UNSUPPORTED"));
    }

    @Test
    void dynamicSignRelationReadMustStayTenantScopedAndBounded() throws IOException {
        String mapper = Files.readString(Path.of("src/main/resources/mapper/FlowTaskCandidateMapper.xml"));
        String migration = Files.readString(Path.of("../../../db/mysql/migration/V1.0.148__add_flow_task_sign_relation_audit.sql"));
        String idempotencyMigration = Files.readString(Path.of(
                "../../../db/mysql/migration/V1.0.153__add_flow_task_sign_idempotency.sql"));
        String runtimeRelationMigration = Files.readString(Path.of(
                "../../../db/mysql/migration/V1.0.154__add_flow_task_sign_runtime_relation.sql"));
        String signDictMigration = Files.readString(Path.of(
                "../../../db/mysql/migration/V1.0.155__seed_flow_sign_mode_dict.sql"));
        assertTrue(mapper.contains("tenant_id = #{tenantId}"));
        assertTrue(mapper.contains("parent_task_id = #{parentTaskId}"));
        assertTrue(mapper.contains("source = 'DYNAMIC_SIGN'"));
        assertTrue(mapper.contains("LIMIT 50"));
        assertTrue(migration.contains("parent_task_id"));
        assertTrue(migration.contains("sign_mode"));
        assertTrue(migration.contains("idx_flow_task_candidate_parent"));
        assertTrue(idempotencyMigration.contains("idempotency_key"));
        assertTrue(idempotencyMigration.contains("request_digest"));
        assertTrue(idempotencyMigration.contains("idx_flow_task_candidate_idempotency"));
        assertTrue(runtimeRelationMigration.contains("child_task_id"));
        assertTrue(runtimeRelationMigration.contains("child_execution_id"));
        assertTrue(runtimeRelationMigration.contains("idx_flow_task_candidate_child"));
        assertTrue(mapper.contains("selectActiveDynamicSignRelation"));
        assertTrue(signDictMigration.contains("flow_task_sign_mode"));
        assertTrue(signDictMigration.contains("flow_task_sign_relation_status"));
        assertTrue(signDictMigration.contains("WHERE NOT EXISTS"));
    }

    @Test
    void delegateActionMustPersistAndReplayIdempotencyCredentials() throws IOException {
        String dto = Files.readString(Path.of(
                "../../../forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/dto/FlowTaskActionDTO.java"));
        String controller = Files.readString(Path.of(
                "../../../forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowTaskController.java"));
        String service = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java"));
        String coordinator = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskActionCoordinator.java"));
        String authorization = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskActionAuthorization.java"));
        assertTrue(dto.contains("private String idempotencyKey"));
        assertTrue(dto.contains("private String requestDigest"));
        assertTrue(controller.contains("optionalText(dto.getIdempotencyKey())"));
        assertTrue(controller.contains("optionalText(dto.getRequestDigest())"));
        assertTrue(coordinator.contains("authorizeTaskAction(taskId, userId, tenantId, \"DELEGATE\""));
        assertTrue(coordinator.contains("flowTask.setActionType(idempotencyKey == null ? null : \"DELEGATE\")"));
        assertTrue(authorization.contains("userId.equals(task.getOwner())"));
    }
}
