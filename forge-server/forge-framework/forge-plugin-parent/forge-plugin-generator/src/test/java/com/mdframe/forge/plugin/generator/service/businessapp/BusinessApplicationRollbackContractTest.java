package com.mdframe.forge.plugin.generator.service.businessapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessApplication rollback boundary contract")
class BusinessApplicationRollbackContractTest {

    @Test
    @DisplayName("application rollback checks physical columns and never executes reverse DDL")
    void rollbackFailsClosedWithoutReverseDdl() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationRollbackService.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("历史版本依赖当前不存在的数据库字段"));
        assertTrue(source.contains("tableMappingService.getTableMapping"));
        assertTrue(source.contains("rollbackForApplication"));
        assertFalse(source.contains("syncDatabase("));
        assertFalse(source.contains("executeCreateTable("));
        assertFalse(source.contains("DROP COLUMN"));
    }
}
