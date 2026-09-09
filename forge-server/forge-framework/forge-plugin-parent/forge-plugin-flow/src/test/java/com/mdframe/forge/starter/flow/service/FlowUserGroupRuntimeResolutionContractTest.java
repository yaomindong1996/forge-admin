package com.mdframe.forge.starter.flow.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FlowUserGroupRuntimeResolutionContractTest {

    @Test
    void organizationIntegrationMustExposeCustomGroupResolution() throws IOException {
        String contract = Files.readString(Path.of("src/main/java/com/mdframe/forge/starter/flow/service/FlowOrgIntegrationService.java"));
        String implementation = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowOrgIntegrationServiceImpl.java"));
        String resolver = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskReceiverResolverImpl.java"));
        String guard = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/security/FlowAccessGuard.java"));

        assertThat(contract).contains("getUserIdsByGroupCode", "getUserGroupCodes");
        assertThat(implementation).contains("flowUserGroupService.resolveUserIdsByCode(groupCode.trim())",
                "flowUserGroupService.resolveGroupCodesByUserId(uid)");
        assertThat(resolver).contains("getUserIdsByGroupCode(group)");
        assertThat(guard).contains("resolveGroupCodesByUserId(userId)");
    }

    @Test
    void spelServiceMustProvideGroupFunctionWithoutExposingVariables() throws IOException {
        String service = Files.readString(Path.of("src/main/java/com/mdframe/forge/starter/flow/service/FlowSpelService.java"));
        assertThat(service).contains("findUsersByGroup(Object groupCode)",
                "getUserIdsByGroupCode(groupCode.toString())",
                "MAX_RESULT_USERS");
    }
}
