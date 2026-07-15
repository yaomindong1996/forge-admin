package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessExtensionStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionExecutionLog;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionVersion;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionTestDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionExecutionLogMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionVersionMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.ExtensionExecutionContext;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.ExtensionExecutionResult;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.ExtensionInputField;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.LowcodeExtensionHandler;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.LowcodeExtensionRegistry;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.ServerBindingExecutor;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessExtensionExecutionService")
class BusinessExtensionExecutionServiceTest {

    private ServerBindingExecutor serverBindingExecutor;

    @AfterEach
    void closeExecutor() {
        if (serverBindingExecutor != null) {
            serverBindingExecutor.close();
        }
    }

    @Test
    @DisplayName("validated visual rule test moves the current draft to TESTED")
    void visualRuleTestMovesDraftToTested() {
        AiBusinessExtension extension = extension(BusinessExtensionStatus.DRAFT);
        AiBusinessExtensionVersion version = version(false, false);
        AtomicReference<String> lifecycleStatus = new AtomicReference<>();
        BusinessExtensionMapper extensionMapper = proxy(BusinessExtensionMapper.class, (method, args) -> {
            if ("selectEntityById".equals(method)) {
                return extension;
            }
            if ("updateLifecycle".equals(method)) {
                lifecycleStatus.set((String) args[2]);
                return 1;
            }
            return defaultValue(method);
        });
        BusinessExtensionVersionMapper versionMapper = proxy(BusinessExtensionVersionMapper.class, (method, args) ->
                "selectVersion".equals(method) ? version : defaultValue(method));
        BusinessExtensionExecutionService service = service(extensionMapper, versionMapper);

        var result = service.test(extension.getId(), new BusinessExtensionTestDTO());

        assertTrue(result.isPassed());
        assertEquals(BusinessExtensionStatus.TESTED, lifecycleStatus.get());
    }

    @Test
    @DisplayName("tested version enables exactly the current draft version")
    void testedVersionEnablesCurrentDraft() {
        AiBusinessExtension extension = extension(BusinessExtensionStatus.TESTED);
        AiBusinessExtensionVersion version = version(true, true);
        AtomicReference<Integer> enabledVersion = new AtomicReference<>();
        BusinessExtensionMapper extensionMapper = proxy(BusinessExtensionMapper.class, (method, args) -> {
            if ("selectEntityById".equals(method)) {
                return extension;
            }
            if ("updateLifecycle".equals(method)) {
                enabledVersion.set((Integer) args[3]);
                return 1;
            }
            return defaultValue(method);
        });
        BusinessExtensionVersionMapper versionMapper = proxy(BusinessExtensionVersionMapper.class, (method, args) ->
                "selectVersion".equals(method) ? version : defaultValue(method));
        BusinessExtensionExecutionService service = service(extensionMapper, versionMapper);

        service.updateStatus(extension.getId(), BusinessExtensionStatus.ENABLED);

        assertEquals(extension.getDraftVersion(), enabledVersion.get());
    }

    @Test
    @DisplayName("a structured Java handler failure fails the extension test and writes failed audit")
    void structuredHandlerFailureFailsTestAndWritesAudit() {
        AiBusinessExtension extension = extension(BusinessExtensionStatus.DRAFT);
        extension.setExtensionCode("purchase_validation");
        extension.setExtensionType("SERVER_BINDING");
        AiBusinessExtensionVersion version = version(false, false);
        version.setContent("{}");
        version.setConfigJson("{\"handlerCode\":\"purchase_validation\"}");
        AtomicReference<AiBusinessExtensionExecutionLog> auditRef = new AtomicReference<>();
        BusinessExtensionMapper extensionMapper = proxy(BusinessExtensionMapper.class, (method, args) ->
                "selectEntityById".equals(method) ? extension : defaultValue(method));
        BusinessExtensionVersionMapper versionMapper = proxy(BusinessExtensionVersionMapper.class, (method, args) ->
                "selectVersion".equals(method) ? version : defaultValue(method));
        BusinessExtensionExecutionLogMapper auditMapper = proxy(BusinessExtensionExecutionLogMapper.class,
                (method, args) -> {
                    if ("insert".equals(method)) {
                        auditRef.set((AiBusinessExtensionExecutionLog) args[0]);
                        return 1;
                    }
                    return defaultValue(method);
                });
        BusinessExtensionExecutionService service = service(
                extensionMapper, versionMapper, List.of(failingHandler()), auditMapper);

        var result = service.test(extension.getId(), new BusinessExtensionTestDTO());

        assertFalse(result.isPassed());
        assertTrue(result.getSummary().contains("HANDLER_REJECTED"));
        assertEquals("FAILED", auditRef.get().getResultStatus());
        assertEquals("HANDLER_REJECTED", auditRef.get().getErrorCode());
    }

    @Test
    @DisplayName("BLOCK policy stops the current business action on a structured handler failure")
    void blockPolicyStopsBusinessAction() {
        AiBusinessExtension extension = serverExtension("BLOCK");
        AiBusinessExtensionVersion version = version(false, false);
        version.setContent("{}");
        version.setConfigJson("{\"handlerCode\":\"purchase_validation\"}");
        BusinessExtensionMapper extensionMapper = proxy(BusinessExtensionMapper.class, (method, args) ->
                "selectEnabledForHook".equals(method) ? List.of(extension) : defaultValue(method));
        BusinessExtensionVersionMapper versionMapper = proxy(BusinessExtensionVersionMapper.class, (method, args) ->
                "selectVersion".equals(method) ? version : defaultValue(method));
        BusinessExtensionExecutionService service = service(
                extensionMapper, versionMapper, List.of(failingHandler()),
                proxy(BusinessExtensionExecutionLogMapper.class, (method, args) -> defaultValue(method)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.executeHook(10L, null, null, "BEFORE_SUBMIT", Map.of()));

        assertTrue(error.getMessage().contains("HANDLER_REJECTED"));
    }

    @Test
    @DisplayName("WARN policy preserves the failed result and continues the business action")
    void warnPolicyPreservesFailureAndContinues() {
        AiBusinessExtension extension = serverExtension("WARN");
        AiBusinessExtensionVersion version = version(false, false);
        version.setContent("{}");
        version.setConfigJson("{\"handlerCode\":\"purchase_validation\"}");
        BusinessExtensionMapper extensionMapper = proxy(BusinessExtensionMapper.class, (method, args) ->
                "selectEnabledForHook".equals(method) ? List.of(extension) : defaultValue(method));
        BusinessExtensionVersionMapper versionMapper = proxy(BusinessExtensionVersionMapper.class, (method, args) ->
                "selectVersion".equals(method) ? version : defaultValue(method));
        BusinessExtensionExecutionService service = service(
                extensionMapper, versionMapper, List.of(failingHandler()),
                proxy(BusinessExtensionExecutionLogMapper.class, (method, args) -> defaultValue(method)));

        List<ExtensionExecutionResult> results = service.executeHook(
                10L, null, null, "BEFORE_SUBMIT", Map.of());

        assertEquals(1, results.size());
        assertFalse(results.get(0).isSuccess());
        assertEquals("HANDLER_REJECTED", results.get(0).getCode());
    }

    private BusinessExtensionExecutionService service(BusinessExtensionMapper extensionMapper,
                                                        BusinessExtensionVersionMapper versionMapper) {
        return service(extensionMapper, versionMapper, List.of(),
                proxy(BusinessExtensionExecutionLogMapper.class, (method, args) -> defaultValue(method)));
    }

    private BusinessExtensionExecutionService service(BusinessExtensionMapper extensionMapper,
                                                        BusinessExtensionVersionMapper versionMapper,
                                                        List<LowcodeExtensionHandler> handlers,
                                                        BusinessExtensionExecutionLogMapper auditMapper) {
        ObjectMapper objectMapper = new ObjectMapper();
        LowcodeExtensionRegistry registry = new LowcodeExtensionRegistry(handlers);
        serverBindingExecutor = new ServerBindingExecutor(registry);
        return new BusinessExtensionExecutionService(
                extensionMapper,
                versionMapper,
                auditMapper,
                new BusinessExtensionValidationService(objectMapper, registry),
                new BusinessExtensionStateMachine(),
                serverBindingExecutor,
                objectMapper,
                changeTracker());
    }

    private LowcodeExtensionHandler failingHandler() {
        return new LowcodeExtensionHandler() {
            @Override
            public String handlerCode() {
                return "purchase_validation";
            }

            @Override
            public String handlerName() {
                return "采购单校验";
            }

            @Override
            public Set<String> allowedHooks() {
                return Set.of("BEFORE_SUBMIT");
            }

            @Override
            public Map<String, ExtensionInputField> inputSchema() {
                return Map.of();
            }

            @Override
            public ExtensionExecutionResult execute(ExtensionExecutionContext context) {
                return ExtensionExecutionResult.failure("HANDLER_REJECTED", "业务校验未通过");
            }
        };
    }

    private BusinessApplicationChangeTracker changeTracker() {
        BusinessApplicationMapper mapper = proxy(BusinessApplicationMapper.class,
                (method, args) -> "markChanged".equals(method) ? 1 : defaultValue(method));
        return new BusinessApplicationChangeTracker(mapper);
    }

    private AiBusinessExtension extension(String status) {
        AiBusinessExtension extension = new AiBusinessExtension();
        extension.setId(20L);
        extension.setTenantId(1L);
        extension.setApplicationId(10L);
        extension.setExtensionCode("visual_validation");
        extension.setExtensionType("VISUAL_RULE");
        extension.setHookCode("BEFORE_SUBMIT");
        extension.setFailurePolicy("BLOCK");
        extension.setRiskLevel("MEDIUM");
        extension.setStatus(status);
        extension.setDraftVersion(2);
        return extension;
    }

    private AiBusinessExtension serverExtension(String failurePolicy) {
        AiBusinessExtension extension = extension(BusinessExtensionStatus.ENABLED);
        extension.setExtensionCode("purchase_validation");
        extension.setExtensionType("SERVER_BINDING");
        extension.setFailurePolicy(failurePolicy);
        extension.setEnabledVersion(2);
        return extension;
    }

    private AiBusinessExtensionVersion version(boolean validated, boolean tested) {
        AiBusinessExtensionVersion version = new AiBusinessExtensionVersion();
        version.setExtensionId(20L);
        version.setVersionNo(2);
        version.setContent("{\"match\":\"ALL\",\"conditions\":[],\"actions\":[{\"actionType\":\"SHOW_MESSAGE\"}]}");
        version.setConfigJson("{}");
        version.setValidationPassed(validated ? 1 : 0);
        version.setTestPassed(tested ? 1 : 0);
        return version;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, ProxyHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + "Proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return handler.invoke(method.getName(), args == null ? new Object[0] : args);
                });
    }

    private static Object defaultValue(String method) {
        return switch (method) {
            case "insert", "updateValidationResult", "updateTestResult", "updateLifecycle" -> 1;
            default -> null;
        };
    }

    @FunctionalInterface
    private interface ProxyHandler {
        Object invoke(String method, Object[] args) throws Throwable;
    }
}
