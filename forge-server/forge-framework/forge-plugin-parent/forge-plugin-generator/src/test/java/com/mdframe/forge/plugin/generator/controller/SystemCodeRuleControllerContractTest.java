package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.domain.entity.AiCodeRule;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSaveDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSegmentDTO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemCodeRuleControllerContractTest {

    @Test
    void shouldExposePostSafeEncryptedManagementContract() throws Exception {
        Class<SystemCodeRuleController> controllerType = SystemCodeRuleController.class;
        assertEquals("/system/code-rule", controllerType.getAnnotation(RequestMapping.class).value()[0]);
        assertNotNull(controllerType.getAnnotation(ApiDecrypt.class));
        assertNotNull(controllerType.getAnnotation(ApiEncrypt.class));

        assertGet(controllerType.getMethod("page", Integer.class, Integer.class, String.class,
                String.class, String.class, Integer.class), "/page", "system:codeRule:list");
        assertGet(controllerType.getMethod("list", String.class, String.class),
                "/list", "system:codeRule:list");
        assertGet(controllerType.getMethod("capabilities", Long.class),
                "/capabilities", "system:codeRule:list");
        assertPost(controllerType.getMethod("getById", Long.class), "/getById", "system:codeRule:list");
        assertPost(controllerType.getMethod("add", CodeRuleSaveDTO.class), "/add", "system:codeRule:add");
        assertPost(controllerType.getMethod("edit", CodeRuleSaveDTO.class), "/edit", "system:codeRule:edit");

        assertFalse(Arrays.stream(controllerType.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .anyMatch(AiCodeRule.class::equals));
    }

    @Test
    void shouldCollectVariableFieldsAndDetectOutOfScopeMappings() {
        CodeRuleSaveDTO dto = variableRule("warehouseCode");
        dto.getSegments().addAll(variableRule("warehouseType").getSegments());

        Set<String> requested = SystemCodeRuleController.variableFieldCodes(dto);
        Set<String> missing = SystemCodeRuleController.missingVariableFieldCodes(
                requested, Set.of("warehouseCode"));

        assertEquals(Set.of("warehouseCode", "warehouseType"), requested);
        assertEquals(Set.of("warehouseType"), missing);
    }

    private CodeRuleSaveDTO variableRule(String fieldCode) {
        CodeRuleSegmentDTO segment = new CodeRuleSegmentDTO();
        segment.setSegmentType("VARIABLE");
        segment.setSegmentValue(fieldCode);
        CodeRuleSaveDTO dto = new CodeRuleSaveDTO();
        dto.setSourceObjectId(100L);
        dto.setSegments(new ArrayList<>(List.of(segment)));
        return dto;
    }

    private void assertGet(Method method, String path, String permission) {
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertNotNull(mapping);
        assertTrue(Arrays.asList(mapping.value()).contains(path));
        assertPermission(method, permission);
    }

    private void assertPost(Method method, String path, String permission) {
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertNotNull(mapping);
        assertTrue(Arrays.asList(mapping.value()).contains(path));
        assertPermission(method, permission);
    }

    private void assertPermission(Method method, String permission) {
        SaCheckPermission annotation = method.getAnnotation(SaCheckPermission.class);
        assertNotNull(annotation);
        assertTrue(Arrays.asList(annotation.value()).contains(permission));
    }
}
