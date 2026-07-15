package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPublishDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationRollbackDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("BusinessApplication Phase 5 controller contract")
class BusinessApplicationPhaseFiveControllerTest {

    @Test
    @DisplayName("publish check and publish use independent permission and idempotency header")
    void publishContract() throws NoSuchMethodException {
        Method check = BusinessApplicationController.class.getDeclaredMethod(
                "publishCheck", Long.class, BusinessApplicationPublishDTO.class);
        Method publish = BusinessApplicationController.class.getDeclaredMethod(
                "publish", Long.class, String.class, BusinessApplicationPublishDTO.class);

        assertArrayEquals(new String[]{"/{id}/publish/check"}, check.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/{id}/publish"}, publish.getAnnotation(PostMapping.class).value());
        assertPermission(check, "ai:businessApplication:publish");
        assertPermission(publish, "ai:businessApplication:publish");
        RequestHeader header = publish.getParameters()[1].getAnnotation(RequestHeader.class);
        assertNotNull(header);
        assertEquals("Idempotency-Key", header.value());
    }

    @Test
    @DisplayName("version history recovery and rollback have explicit routes")
    void historyRecoveryContract() throws NoSuchMethodException {
        Method versions = BusinessApplicationController.class.getDeclaredMethod("versions", Long.class);
        Method runs = BusinessApplicationController.class.getDeclaredMethod("publishRuns", Long.class);
        Method recover = BusinessApplicationController.class.getDeclaredMethod("recover", Long.class, Long.class);
        Method rollback = BusinessApplicationController.class.getDeclaredMethod(
                "rollback", Long.class, Integer.class, String.class, BusinessApplicationRollbackDTO.class);

        assertArrayEquals(new String[]{"/{id}/versions"}, versions.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[]{"/{id}/publish-runs"}, runs.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[]{"/{id}/publish-runs/{runId}/recover"},
                recover.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/{id}/versions/{versionNo}/rollback"},
                rollback.getAnnotation(PostMapping.class).value());
        assertPermission(recover, "ai:businessApplication:recover");
        assertPermission(rollback, "ai:businessApplication:rollback");
    }

    private void assertPermission(Method method, String permission) {
        SaCheckPermission annotation = method.getAnnotation(SaCheckPermission.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[]{permission}, annotation.value());
    }
}
