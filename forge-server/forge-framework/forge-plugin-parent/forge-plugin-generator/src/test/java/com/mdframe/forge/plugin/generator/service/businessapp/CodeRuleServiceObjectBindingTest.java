package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiCodeRule;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodeRuleServiceObjectBindingTest {

    private final CodeRuleService service = new CodeRuleService(null, null, null);

    @Test
    void shouldRejectMissingOrMismatchedLowCodeObjectContext() throws Exception {
        AiCodeRule rule = new AiCodeRule();
        rule.setSourceObjectCode("purchase_order");
        Method method = CodeRuleService.class.getDeclaredMethod(
                "validateSourceObjectContext", AiCodeRule.class, Map.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(service, rule, Map.of("objectCode", "purchase_order")));
        assertBusinessException(method, rule, Map.of());
        assertBusinessException(method, rule, Map.of("objectCode", "warehouse"));
    }

    private void assertBusinessException(Method method,
                                         AiCodeRule rule,
                                         Map<String, Object> fields) {
        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(service, rule, fields));
        assertInstanceOf(BusinessException.class, exception.getCause());
    }
}
