package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiCodeRule;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSaveDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSegmentDTO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void shouldAllowGenericCustomVariableRuleWithoutObjectContext() throws Exception {
        AiCodeRule rule = new AiCodeRule();
        Method method = CodeRuleService.class.getDeclaredMethod(
                "validateSourceObjectContext", AiCodeRule.class, Map.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(service, rule, Map.of("customerType", "VIP")));
    }

    @Test
    void shouldClearStaleObjectBindingWhenSavingCustomVariables() throws Exception {
        CodeRuleSaveDTO dto = variableRule("CUSTOM");
        dto.setSourceObjectId(100L);
        dto.setSourceObjectCode("purchase_order");
        Method method = CodeRuleService.class.getDeclaredMethod(
                "normalizeAndValidateSave", CodeRuleSaveDTO.class, boolean.class);
        method.setAccessible(true);

        method.invoke(service, dto, true);

        assertNull(dto.getSourceObjectId());
        assertNull(dto.getSourceObjectCode());
    }

    @Test
    void shouldRequireEnrichedObjectBindingWhenSavingLowCodeVariables() throws Exception {
        CodeRuleSaveDTO dto = variableRule("LOWCODE");
        dto.setSourceObjectId(100L);
        Method method = CodeRuleService.class.getDeclaredMethod(
                "normalizeAndValidateSave", CodeRuleSaveDTO.class, boolean.class);
        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(service, dto, true));
        assertInstanceOf(BusinessException.class, exception.getCause());
    }

    private CodeRuleSaveDTO variableRule(String variableSource) {
        CodeRuleSegmentDTO segment = new CodeRuleSegmentDTO();
        segment.setSegmentType("VARIABLE");
        segment.setVariableSource(variableSource);
        segment.setSegmentValue("customerType");

        CodeRuleSaveDTO dto = new CodeRuleSaveDTO();
        dto.setRuleCode("customer_code");
        dto.setRuleName("客户编码");
        dto.setCategory("CUSTOMER");
        dto.setSegments(List.of(segment));
        return dto;
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
