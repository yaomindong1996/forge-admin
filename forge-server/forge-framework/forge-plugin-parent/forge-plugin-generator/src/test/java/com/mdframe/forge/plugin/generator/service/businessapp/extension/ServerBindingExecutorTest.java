package com.mdframe.forge.plugin.generator.service.businessapp.extension;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ServerBindingExecutor")
class ServerBindingExecutorTest {

    private ServerBindingExecutor executor;

    @AfterEach
    void closeExecutor() {
        if (executor != null) {
            executor.close();
        }
    }

    @Test
    @DisplayName("registered handler executes after hook and schema validation")
    void registeredHandlerExecutes() {
        executor = executor(handler("safe_handler", 200, false));

        ExtensionExecutionResult result = executor.execute(context("safe_handler", "BEFORE_SUBMIT",
                Map.of("customerId", 12L)));

        assertTrue(result.isSuccess());
        assertEquals(true, result.getOutput().get("accepted"));
    }

    @Test
    @DisplayName("unknown class or bean names cannot be resolved")
    void unknownHandlerFailsClosed() {
        executor = executor(handler("safe_handler", 200, false));

        assertThrows(BusinessException.class,
                () -> executor.execute(context("java.lang.Runtime", "BEFORE_SUBMIT", Map.of("customerId", 12L))));
    }

    @Test
    @DisplayName("unknown input fields and invalid hooks are rejected")
    void schemaAndHookViolationsAreRejected() {
        executor = executor(handler("safe_handler", 200, false));

        assertThrows(BusinessException.class,
                () -> executor.execute(context("safe_handler", "AFTER_DELETE", Map.of("customerId", 12L))));
        assertThrows(BusinessException.class,
                () -> executor.execute(context("safe_handler", "BEFORE_SUBMIT",
                        Map.of("customerId", 12L, "className", "java.lang.Runtime"))));
    }

    @Test
    @DisplayName("handler timeout is cancelled and returns a redacted error")
    void timeoutFailsClosed() {
        executor = executor(handler("slow_handler", 20, true));

        BusinessException error = assertThrows(BusinessException.class,
                () -> executor.execute(context("slow_handler", "BEFORE_SUBMIT", Map.of("customerId", 12L))));

        assertTrue(error.getMessage().contains("超时"));
        assertTrue(!error.getMessage().contains("secret-customer-payload"));
    }

    private ServerBindingExecutor executor(LowcodeExtensionHandler handler) {
        return new ServerBindingExecutor(new LowcodeExtensionRegistry(List.of(handler)));
    }

    private ExtensionExecutionContext context(String handlerCode, String hookCode, Map<String, Object> input) {
        ExtensionExecutionContext context = new ExtensionExecutionContext();
        context.setTenantId(1L);
        context.setApplicationId(10L);
        context.setExtensionId(20L);
        context.setHandlerCode(handlerCode);
        context.setHookCode(hookCode);
        context.setInput(input);
        return context;
    }

    private LowcodeExtensionHandler handler(String code, int timeoutMs, boolean slow) {
        return new LowcodeExtensionHandler() {
            @Override
            public String handlerCode() {
                return code;
            }

            @Override
            public String handlerName() {
                return "安全处理器";
            }

            @Override
            public Set<String> allowedHooks() {
                return Set.of("BEFORE_SUBMIT");
            }

            @Override
            public Map<String, ExtensionInputField> inputSchema() {
                return Map.of("customerId", ExtensionInputField.required("LONG"));
            }

            @Override
            public int timeoutMs() {
                return timeoutMs;
            }

            @Override
            public ExtensionExecutionResult execute(ExtensionExecutionContext context) {
                if (slow) {
                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("secret-customer-payload", e);
                    }
                }
                return ExtensionExecutionResult.success(Map.of("accepted", true));
            }
        };
    }
}
