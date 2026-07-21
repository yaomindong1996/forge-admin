package com.mdframe.forge.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JobFlowControllerSecurityContractTest {

    @Test
    void shouldRequireJobTriggerPermissionForTechnicalFlowEndpoints() {
        SaCheckPermission permission = JobFlowController.class.getAnnotation(SaCheckPermission.class);

        assertNotNull(permission);
        assertArrayEquals(new String[]{"system:jobConfig:trigger"}, permission.value());
    }
}
