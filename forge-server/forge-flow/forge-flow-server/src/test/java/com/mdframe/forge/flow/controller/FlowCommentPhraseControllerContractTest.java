package com.mdframe.forge.flow.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FlowCommentPhraseControllerContractTest {

    private static final Path CONTROLLER = Path.of(
            "src/main/java/com/mdframe/forge/flow/controller/FlowCommentPhraseController.java");

    @Test
    void writeEndpointsMustUseTypedDtos() throws IOException {
        String controller = Files.readString(CONTROLLER);
        assertThat(controller)
                .contains("FlowCommentPhraseCreateDTO",
                        "FlowCommentPhraseUpdateDTO",
                        "FlowCommentPhraseQuery",
                        "@RequestBody")
                .doesNotContain("RequestBody Map", "Map<String, Object>");
    }

    @Test
    void usableAndPersonalWriteMustIgnoreApiPermission() throws IOException {
        String controller = Files.readString(CONTROLLER);
        assertThat(controller).contains(
                "@RequestMapping(\"/api/flow/comment-phrases\")",
                "@GetMapping(\"/usable\")",
                "@GetMapping(\"/mine\")",
                "@ApiPermissionIgnore",
                "@SaCheckPermission(\"flow:comment-phrase:view\")",
                "@PostMapping",
                "@PutMapping",
                "@DeleteMapping(\"/{id}\")");
    }
}
