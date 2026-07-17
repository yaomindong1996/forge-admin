package com.mdframe.forge.starter.id.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SequenceControllerContractTest {

    @Test
    void sequenceApiShouldBeDisabledByDefaultAndRequireDedicatedPermission() throws IOException {
        String source = Files.readString(resolveSource());

        assertTrue(source.contains("matchIfMissing = false"));
        assertTrue(source.contains("@SaCheckPermission(\"system:sequence:use\")"));
        assertFalse(source.contains("@GetMapping"));
        assertTrue(source.contains("@PostMapping(\"/next\")"));
        assertTrue(source.contains("@PostMapping(\"/nextBatch\")"));
        assertTrue(source.contains("@PostMapping(\"/nextFormatted\")"));
        assertTrue(source.contains("@PostMapping(\"/nextFormattedBatch\")"));
    }

    @Test
    void sequenceApiShouldValidateUserSuppliedBusinessKey() throws IOException {
        String source = Files.readString(resolveSource());

        assertTrue(source.contains("@Pattern("));
        assertTrue(source.contains("@Size(max = 100"));
    }

    private Path resolveSource() {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            Path candidate = current.resolve(
                    "src/main/java/com/mdframe/forge/starter/id/controller/SequenceController.java");
            if (Files.exists(candidate)) {
                return candidate;
            }
            candidate = current.resolve(
                    "forge-server/forge-framework/forge-starter-parent/forge-starter-id"
                            + "/src/main/java/com/mdframe/forge/starter/id/controller/SequenceController.java");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Path.of("src/main/java/com/mdframe/forge/starter/id/controller/SequenceController.java");
    }
}
