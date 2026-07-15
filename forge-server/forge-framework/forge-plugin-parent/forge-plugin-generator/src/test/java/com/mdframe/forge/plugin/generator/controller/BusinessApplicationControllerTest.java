package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationQueryDTO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("BusinessApplicationController contract")
class BusinessApplicationControllerTest {

    @Test
    @DisplayName("application API has an independent encrypted namespace")
    void applicationApiHasIndependentEncryptedNamespace() {
        RequestMapping mapping = BusinessApplicationController.class.getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/ai/business/application"}, mapping.value());
        assertNotNull(BusinessApplicationController.class.getAnnotation(ApiDecrypt.class));
        assertNotNull(BusinessApplicationController.class.getAnnotation(ApiEncrypt.class));
    }

    @Test
    @DisplayName("page uses pageNum and pageSize defaults")
    void pageUsesForgePaginationContract() throws NoSuchMethodException {
        Method method = BusinessApplicationController.class.getDeclaredMethod(
                "page", Integer.class, Integer.class, BusinessApplicationQueryDTO.class);
        Parameter[] parameters = method.getParameters();

        assertArrayEquals(new String[]{"/page"}, method.getAnnotation(GetMapping.class).value());
        assertEquals("1", parameters[0].getAnnotation(RequestParam.class).defaultValue());
        assertEquals("10", parameters[1].getAnnotation(RequestParam.class).defaultValue());
        assertPermission(method, "ai:businessApplication:list");
    }

    @Test
    @DisplayName("CRUD endpoints use separate aggregate permissions")
    void crudEndpointsUseAggregatePermissions() throws NoSuchMethodException {
        Method create = BusinessApplicationController.class.getDeclaredMethod("create", BusinessApplicationDTO.class);
        Method update = BusinessApplicationController.class.getDeclaredMethod("update", BusinessApplicationDTO.class);
        Method status = BusinessApplicationController.class.getDeclaredMethod("updateStatus", Long.class, Integer.class);
        Method delete = BusinessApplicationController.class.getDeclaredMethod("delete", Long.class);

        assertNotNull(create.getAnnotation(PostMapping.class));
        assertNotNull(update.getAnnotation(PutMapping.class));
        assertArrayEquals(new String[]{"/{id}/status"}, status.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[]{"/{id}"}, delete.getAnnotation(DeleteMapping.class).value());
        assertPermission(create, "ai:businessApplication:add");
        assertPermission(update, "ai:businessApplication:edit");
        assertPermission(status, "ai:businessApplication:status");
        assertPermission(delete, "ai:businessApplication:delete");
    }

    @Test
    @DisplayName("object composition endpoints retain list and edit permission boundaries")
    void objectCompositionEndpointsUseApprovedPermissions() throws NoSuchMethodException {
        Method listObjects = BusinessApplicationController.class.getDeclaredMethod("listObjects", Long.class);
        Method replaceObjects = BusinessApplicationController.class.getDeclaredMethod(
                "replaceObjects", Long.class, List.class);

        assertArrayEquals(new String[]{"/{id}/objects"}, listObjects.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[]{"/{id}/objects"}, replaceObjects.getAnnotation(PutMapping.class).value());
        assertPermission(listObjects, "ai:businessApplication:list");
        assertPermission(replaceObjects, "ai:businessApplication:edit");
    }

    @Test
    @DisplayName("workspace and readiness are independent lazy summary endpoints")
    void workspaceEndpointsUseListPermission() throws NoSuchMethodException {
        Method workspace = BusinessApplicationController.class.getDeclaredMethod("workspace", Long.class);
        Method readiness = BusinessApplicationController.class.getDeclaredMethod("readiness", Long.class);

        assertArrayEquals(new String[]{"/{id}/workspace"}, workspace.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[]{"/{id}/readiness"}, readiness.getAnnotation(GetMapping.class).value());
        assertPermission(workspace, "ai:businessApplication:list");
        assertPermission(readiness, "ai:businessApplication:list");
    }

    private void assertPermission(Method method, String permission) {
        SaCheckPermission annotation = method.getAnnotation(SaCheckPermission.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[]{permission}, annotation.value());
    }
}
