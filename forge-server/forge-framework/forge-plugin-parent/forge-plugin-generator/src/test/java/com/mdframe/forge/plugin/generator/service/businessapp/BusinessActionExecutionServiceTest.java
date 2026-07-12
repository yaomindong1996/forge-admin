package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessActionExecutionLog;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessActionExecuteDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessActionExecutionLogMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectActionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessActionStepResultVO;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@DisplayName("BusinessActionExecutionService")
class BusinessActionExecutionServiceTest {

    private final DynamicCrudService dynamicCrudService = Mockito.mock(DynamicCrudService.class);
    private final BusinessObjectActionService actionService = Mockito.mock(BusinessObjectActionService.class);
    private final BusinessActionExecutionLogMapper logMapper = Mockito.mock(BusinessActionExecutionLogMapper.class);
    private final BusinessActionStepExecutor stepExecutor = Mockito.mock(BusinessActionStepExecutor.class);
    private final BusinessActionExecutionService service = new BusinessActionExecutionService(
            new ObjectMapper(),
            dynamicCrudService,
            actionService,
            logMapper,
            new TestTransactionManager(),
            List.of(stepExecutor));

    @Test
    @DisplayName("duplicate running idempotency key does not execute action steps")
    void duplicateRunningIdempotencyKeyDoesNotExecuteSteps() {
        AiBusinessObject object = businessObject();
        BusinessObjectActionVO action = actionWithNoopStep();
        when(actionService.resolveAction(null, "order", "confirm"))
                .thenReturn(new BusinessObjectActionService.ResolvedBusinessAction(object, action));
        AiBusinessActionExecutionLog concurrentLog = runningLog();
        Mockito.doAnswer(invocation -> {
                    AiBusinessActionExecutionLog reserved = invocation.getArgument(0);
                    concurrentLog.setRequestDigest(reserved.getRequestDigest());
                    throw new DuplicateKeyException("duplicate idempotency key");
                })
                .when(logMapper).insert(any(AiBusinessActionExecutionLog.class));
        when(logMapper.selectLatestByIdempotencyKey(eq(1L), eq("order"), eq("1001"), eq("confirm"), eq("K1")))
                .thenReturn(null, concurrentLog);
        when(stepExecutor.supportType()).thenReturn("NOOP");

        BusinessActionExecuteDTO dto = new BusinessActionExecuteDTO();
        dto.setObjectCode("order");
        dto.setRecordId("1001");
        dto.setActionCode("confirm");
        dto.setIdempotencyKey("K1");

        BusinessException error = assertThrows(BusinessException.class, () -> service.execute(dto));

        assertEquals("业务动作正在执行，请稍候", error.getMessage());
        verify(stepExecutor, never()).execute(any(), any());
        verify(logMapper, times(1)).insert(any(AiBusinessActionExecutionLog.class));
    }

    @Test
    @DisplayName("same idempotency key cannot be reused with different arguments")
    void sameIdempotencyKeyCannotBeReusedWithDifferentArguments() {
        AiBusinessObject object = businessObject();
        BusinessObjectActionVO action = actionWithNoopStep();
        when(actionService.resolveAction(null, "order", "confirm"))
                .thenReturn(new BusinessObjectActionService.ResolvedBusinessAction(object, action));
        AiBusinessActionExecutionLog existing = runningLog();
        existing.setExecuteStatus("SUCCESS");
        existing.setRequestDigest("sha256:different");
        when(logMapper.selectLatestByIdempotencyKey(
                eq(1L), eq("order"), eq("1001"), eq("confirm"), eq("K1")))
                .thenReturn(existing);

        BusinessActionExecuteDTO dto = new BusinessActionExecuteDTO();
        dto.setObjectCode("order");
        dto.setRecordId("1001");
        dto.setActionCode("confirm");
        dto.setIdempotencyKey("K1");
        dto.setFormData(Map.of("status", "CONFIRMED"));

        BusinessException error = assertThrows(BusinessException.class, () -> service.execute(dto));

        assertEquals("幂等键已被不同业务动作参数使用，请更换幂等键", error.getMessage());
        verify(stepExecutor, never()).execute(any(), any());
    }

    @Test
    @DisplayName("resolves object code aliases and runtime row context")
    void resolvesObjectCodeAliasesAndRuntimeRowContext() {
        BusinessActionExecuteDTO dto = new BusinessActionExecuteDTO();
        dto.setBusinessObjectCode("purchase_order");
        assertEquals("purchase_order", service.resolveObjectCode(dto));

        dto = new BusinessActionExecuteDTO();
        dto.setTargetObjectCode("supplier");
        assertEquals("supplier", service.resolveObjectCode(dto));

        dto = new BusinessActionExecuteDTO();
        dto.setContext(Map.of("row", Map.of("_runtimeObjectCode", "outbound_order")));
        assertEquals("outbound_order", service.resolveObjectCode(dto));

        dto = new BusinessActionExecuteDTO();
        dto.setFormData(Map.of("businessObjectCode", "transfer_order"));
        assertEquals("transfer_order", service.resolveObjectCode(dto));
    }

    @Test
    @DisplayName("published execution log carries trusted delegated identity")
    void publishedExecutionLogCarriesTrustedDelegatedIdentity() {
        AiBusinessObject object = businessObject();
        BusinessObjectActionVO action = actionWithNoopStep();
        when(actionService.resolvePublishedAction(null, "order", "confirm", 3))
                .thenReturn(new BusinessObjectActionService.ResolvedPublishedBusinessAction(
                        object, action, new com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectDesignVersion()));
        when(stepExecutor.supportType()).thenReturn("NOOP");
        BusinessActionStepResultVO stepResult = new BusinessActionStepResultVO();
        stepResult.setStepCode("noop");
        stepResult.setStepType("NOOP");
        stepResult.setStatus("SUCCESS");
        when(stepExecutor.execute(any(), any())).thenReturn(stepResult);
        Mockito.doAnswer(invocation -> {
            AiBusinessActionExecutionLog log = invocation.getArgument(0);
            log.setId(100L);
            return 1;
        }).when(logMapper).insert(any(AiBusinessActionExecutionLog.class));
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        user.setPermissions(Set.of());
        BusinessActionExecuteDTO dto = new BusinessActionExecuteDTO();
        dto.setObjectCode("order");
        dto.setActionCode("confirm");
        dto.setIdempotencyKey("order-confirm-1001");

        try (ExecutionIdentityContextHolder.Scope ignored = ExecutionIdentityContextHolder.open(
                new ExecutionIdentity(user, "USER", 101L, 999L, 301L,
                        "agent_client", "token-1", Set.of("capability:invoke")))) {
            service.executePublished(dto, 3, "req-1");
        }

        ArgumentCaptor<AiBusinessActionExecutionLog> captor =
                ArgumentCaptor.forClass(AiBusinessActionExecutionLog.class);
        verify(logMapper).insert(captor.capture());
        AiBusinessActionExecutionLog log = captor.getValue();
        assertEquals("req-1", log.getCapabilityRequestId());
        assertEquals(301L, log.getClientId());
        assertEquals(999L, log.getServiceUserId());
        assertEquals("USER", log.getActorType());
    }

    private AiBusinessObject businessObject() {
        AiBusinessObject object = new AiBusinessObject();
        object.setTenantId(1L);
        object.setSuiteCode("default");
        object.setObjectCode("order");
        object.setObjectName("订单");
        object.setConfigKey("runtime_order");
        return object;
    }

    private BusinessObjectActionVO actionWithNoopStep() {
        BusinessObjectActionVO action = new BusinessObjectActionVO();
        action.setActionCode("confirm");
        action.setActionName("确认");
        action.setStatus(1);
        action.setActionConfig(Map.of("steps", List.of(Map.of(
                "stepCode", "noop",
                "stepName", "空步骤",
                "stepType", "NOOP"
        ))));
        return action;
    }

    private AiBusinessActionExecutionLog runningLog() {
        AiBusinessActionExecutionLog log = new AiBusinessActionExecutionLog();
        log.setId(10L);
        log.setTenantId(1L);
        log.setSuiteCode("default");
        log.setObjectCode("order");
        log.setRecordId("1001");
        log.setActionCode("confirm");
        log.setActionName("确认");
        log.setExecuteStatus("RUNNING");
        log.setIdempotencyKey("K1");
        log.setResultMessage("动作执行中");
        return log;
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
