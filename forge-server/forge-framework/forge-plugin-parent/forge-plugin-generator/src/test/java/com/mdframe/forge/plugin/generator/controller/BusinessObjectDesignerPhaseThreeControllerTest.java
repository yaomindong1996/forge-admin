package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDatabaseSyncDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("BusinessObjectDesigner Phase 3 controller contract")
class BusinessObjectDesignerPhaseThreeControllerTest {

    @Test
    @DisplayName("table mapping is a read-only design endpoint")
    void tableMappingUsesDesignPermission() throws NoSuchMethodException {
        Method method = BusinessObjectDesignerController.class.getDeclaredMethod("tableMapping", Long.class);

        assertArrayEquals(new String[]{"/{objectId}/table-mapping"}, method.getAnnotation(GetMapping.class).value());
        assertPermission(method, "ai:businessObject:design");
    }

    @Test
    @DisplayName("database preview and sync use separate permission boundaries")
    void databasePreviewAndSyncUseSeparatePermissions() throws NoSuchMethodException {
        Method preview = BusinessObjectDesignerController.class.getDeclaredMethod(
                "databaseDiff", Long.class, BusinessObjectDatabaseSyncDTO.class);
        Method sync = BusinessObjectDesignerController.class.getDeclaredMethod(
                "databaseSync", Long.class, BusinessObjectDatabaseSyncDTO.class);

        assertArrayEquals(new String[]{"/{objectId}/database-diff"}, preview.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/{objectId}/database-sync"}, sync.getAnnotation(PostMapping.class).value());
        assertPermission(preview, "ai:businessObject:design");
        assertPermission(sync, "ai:lowcode:deploy-ddl");
    }

    private void assertPermission(Method method, String permission) {
        SaCheckPermission annotation = method.getAnnotation(SaCheckPermission.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[]{permission}, annotation.value());
    }
}
