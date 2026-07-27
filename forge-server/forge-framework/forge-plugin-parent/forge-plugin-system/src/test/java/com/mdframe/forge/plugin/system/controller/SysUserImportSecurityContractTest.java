package com.mdframe.forge.plugin.system.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SysUserImportSecurityContractTest {

    @Test
    void importEndpointShouldLogInternalExceptionsAndReturnStableMessages() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/system/controller/SysUserController.java"));
        String importMethod = source.substring(
                source.indexOf("public RespInfo<ImportResult<?>> importUsers"),
                source.indexOf("private SysUserDTO buildUserDtoFromRow"));

        assertThat(importMethod)
                .contains("log.error(\"导入用户第{}行写入失败\"",
                        "error.setErrorMessage(\"用户数据写入失败\")",
                        "log.error(\"用户导入失败\", e)",
                        "result.setSummary(ImportResult.PUBLIC_FAILURE_MESSAGE)")
                .doesNotContain("e.getMessage()", "exception.getMessage()");
    }
}
