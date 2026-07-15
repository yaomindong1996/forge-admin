package com.mdframe.forge.plugin.generator.service.businessapp.extension;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("LowcodeExtensionRegistry")
class ServerBindingRegistryTest {

    @Test
    @DisplayName("registered handlers are selected only by stable handler code")
    void registeredHandlersAreSelectedByCode() {
        LowcodeExtensionHandler handler = handler("customer_validation");
        LowcodeExtensionRegistry registry = new LowcodeExtensionRegistry(List.of(handler));

        assertEquals(handler, registry.require("customer_validation"));
        assertThrows(BusinessException.class, () -> registry.require(handler.getClass().getName()));
        assertThrows(BusinessException.class, () -> registry.require("customerValidationBean"));
    }

    @Test
    @DisplayName("duplicate handler codes fail application startup")
    void duplicateHandlerCodesFailClosed() {
        assertThrows(IllegalStateException.class,
                () -> new LowcodeExtensionRegistry(List.of(handler("same_code"), handler("same_code"))));
    }

    private LowcodeExtensionHandler handler(String code) {
        return new LowcodeExtensionHandler() {
            @Override
            public String handlerCode() {
                return code;
            }

            @Override
            public String handlerName() {
                return "客户校验";
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
            public ExtensionExecutionResult execute(ExtensionExecutionContext context) {
                return ExtensionExecutionResult.success(Map.of("accepted", true));
            }
        };
    }
}
