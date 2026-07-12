package com.mdframe.forge.plugin.capability.controlplane.controller;

import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilityClientControllerSecurityTest {

    @Test
    void shouldNeverPersistOneTimeSecretInOperationLogResponse() throws Exception {
        OperationLog addLog = CapabilityClientController.class
                .getMethod("add", com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityClientCreateDTO.class)
                .getAnnotation(OperationLog.class);
        OperationLog rotateLog = CapabilityClientController.class
                .getMethod("rotate", Long.class)
                .getAnnotation(OperationLog.class);

        assertThat(addLog.saveResponseResult()).isFalse();
        assertThat(rotateLog.saveResponseResult()).isFalse();
    }
}
