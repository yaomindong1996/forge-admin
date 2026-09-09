package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static contract checks for the manual CC send boundary. Flow callbacks may
 * send system notifications without an HTTP session, while the controller
 * path must use the session-bound authorization entry point.
 */
class FlowCcSendSecurityContractTest {

    @Test
    void manualSendMustUseSessionBoundAuthorizationAndValidateFlowContext() throws IOException {
        String controller = Files.readString(Path.of(
                "../../../forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowCcController.java"));
        String service = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowCcServiceImpl.java"));

        assertTrue(controller.contains("flowCcService.sendCcByCurrentUser"));
        assertTrue(service.contains("requireUserId()"));
        assertTrue(service.contains("selectByProcessInstanceIdAndTenantId"));
        assertTrue(service.contains("countProcessParticipant"));
        assertTrue(service.contains("selectByIdOrTaskIdAndTenant"));
        assertTrue(service.contains("isUserAvailableForTenant"));
        assertTrue(service.contains("FLOW_CC_SENDER_MISMATCH"));
        assertTrue(service.contains("FLOW_CC_TARGET_INVALID"));
    }
}
