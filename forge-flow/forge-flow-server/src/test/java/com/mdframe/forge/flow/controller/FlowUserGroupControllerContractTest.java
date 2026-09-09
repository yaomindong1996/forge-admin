package com.mdframe.forge.flow.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FlowUserGroupControllerContractTest {

    private static final Path CONTROLLER = Path.of(
            "src/main/java/com/mdframe/forge/flow/controller/FlowUserGroupController.java");

    @Test
    void writeEndpointsMustUseTypedDtosAndSeparatedPermissions() throws IOException {
        String controller = Files.readString(CONTROLLER);
        assertThat(controller)
                .contains("@SaCheckPermission(\"flow:org:group:view\")",
                        "@SaCheckPermission(\"flow:org:group:manage\")",
                        "FlowUserGroupCreateDTO",
                        "FlowUserGroupUpdateDTO",
                        "FlowUserGroupMembersDTO",
                        "@RequestBody")
                .doesNotContain("RequestBody Map", "Map<String, Object>");
    }

    @Test
    void controllerMustExposeGroupAndMemberLifecycle() throws IOException {
        String controller = Files.readString(CONTROLLER);
        assertThat(controller).contains(
                "@RequestMapping(\"/api/flow/org/groups\")",
                "@GetMapping(\"/page\")",
                "@PostMapping",
                "@PutMapping",
                "@DeleteMapping(\"/{id}\")",
                "@GetMapping(\"/{id}/members\")",
                "@PostMapping(\"/{id}/members\")",
                "@DeleteMapping(\"/{id}/members\")");
    }
}
