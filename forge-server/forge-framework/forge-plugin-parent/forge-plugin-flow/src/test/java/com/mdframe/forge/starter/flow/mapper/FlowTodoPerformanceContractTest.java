package com.mdframe.forge.starter.flow.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowTodoPerformanceContractTest {

    private static final String MIGRATION = "V1.0.167__optimize_flow_todo_candidate_lookup.sql";

    @Test
    void candidateVisibilityIndexMustCoverTaskIdAndBeRepeatable() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("information_schema.STATISTICS"));
        assertTrue(sql.contains("INDEX_NAME = 'idx_flow_task_candidate_visibility'"));
        assertTrue(sql.contains("tenant_id, status, candidate_type, candidate_value, task_id"));
        assertFalse(sql.contains("${"));
    }

    @Test
    void taskFormInfoMustExposeTheAuthorizedTaskSnapshot() throws IOException {
        String dto = Files.readString(resolveModuleFile(
                "src/main/java/com/mdframe/forge/starter/flow/dto/TaskFormInfo.java"));
        String service = Files.readString(resolveModuleFile(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskFormContextCoordinator.java"));

        assertTrue(dto.contains("private Integer status;"));
        assertTrue(dto.contains("private String assignee;"));
        assertTrue(dto.contains("private String candidateUsers;"));
        assertTrue(dto.contains("private String candidateGroups;"));
        assertTrue(service.contains("formInfo.setStatus(visibleTask.getStatus())"));
        assertTrue(service.contains("formInfo.setAssignee(visibleTask.getAssignee())"));
    }

    private Path resolveMigration() {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 10 && current != null; depth++) {
            Path candidate = current.resolve("db/mysql/migration").resolve(MIGRATION);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            candidate = current.resolve("forge-server/db/mysql/migration").resolve(MIGRATION);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Path.of("db/mysql/migration").resolve(MIGRATION);
    }

    private Path resolveModuleFile(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            Path candidate = current.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Path.of(relativePath);
    }
}
