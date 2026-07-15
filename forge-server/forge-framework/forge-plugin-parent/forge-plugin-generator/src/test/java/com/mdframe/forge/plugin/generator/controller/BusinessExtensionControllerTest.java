package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionTestDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionVersionDTO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("BusinessExtensionController contract")
class BusinessExtensionControllerTest {

    @Test
    @DisplayName("extension governance uses an encrypted independent namespace")
    void encryptedNamespace() {
        RequestMapping mapping = BusinessExtensionController.class.getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/ai/business/extension"}, mapping.value());
        assertNotNull(BusinessExtensionController.class.getAnnotation(ApiDecrypt.class));
        assertNotNull(BusinessExtensionController.class.getAnnotation(ApiEncrypt.class));
    }

    @Test
    @DisplayName("page follows pageNum and pageSize contract")
    void paginationContract() throws NoSuchMethodException {
        Method method = BusinessExtensionController.class.getDeclaredMethod(
                "page", Integer.class, Integer.class, BusinessExtensionQueryDTO.class);
        Parameter[] parameters = method.getParameters();

        assertArrayEquals(new String[]{"/page"}, method.getAnnotation(GetMapping.class).value());
        assertEquals("1", parameters[0].getAnnotation(RequestParam.class).defaultValue());
        assertEquals("10", parameters[1].getAnnotation(RequestParam.class).defaultValue());
        assertPermission(method, "ai:businessExtension:list");
    }

    @Test
    @DisplayName("save validate test enable and rollback have separate permissions")
    void lifecyclePermissions() throws NoSuchMethodException {
        Method create = BusinessExtensionController.class.getDeclaredMethod("create", BusinessExtensionDTO.class);
        Method saveDraft = BusinessExtensionController.class.getDeclaredMethod(
                "saveDraft", Long.class, BusinessExtensionVersionDTO.class);
        Method validate = BusinessExtensionController.class.getDeclaredMethod("validate", Long.class);
        Method test = BusinessExtensionController.class.getDeclaredMethod(
                "test", Long.class, BusinessExtensionTestDTO.class);
        Method status = BusinessExtensionController.class.getDeclaredMethod(
                "updateStatus", Long.class, String.class);
        Method rollback = BusinessExtensionController.class.getDeclaredMethod(
                "rollback", Long.class, Integer.class, String.class);

        assertNotNull(create.getAnnotation(PostMapping.class));
        assertArrayEquals(new String[]{"/{id}/versions"}, saveDraft.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/{id}/validate"}, validate.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/{id}/test"}, test.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/{id}/status"}, status.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[]{"/{id}/versions/{versionNo}/rollback"},
                rollback.getAnnotation(PostMapping.class).value());
        assertPermission(create, "ai:businessExtension:add");
        assertPermission(saveDraft, "ai:businessExtension:edit");
        assertPermission(validate, "ai:businessExtension:validate");
        assertPermission(test, "ai:businessExtension:test");
        assertPermission(status, "ai:businessExtension:status");
        assertPermission(rollback, "ai:businessExtension:rollback");
    }

    private void assertPermission(Method method, String permission) {
        SaCheckPermission annotation = method.getAnnotation(SaCheckPermission.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[]{permission}, annotation.value());
    }
}
