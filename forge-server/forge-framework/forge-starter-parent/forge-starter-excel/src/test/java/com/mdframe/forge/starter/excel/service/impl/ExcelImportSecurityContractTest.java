package com.mdframe.forge.starter.excel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.excel.model.GenericRowData;
import com.mdframe.forge.starter.excel.model.ImportResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelImportSecurityContractTest {

    @Test
    void unexpectedImportFailureShouldReturnOnlyStablePublicMessage() {
        String internalMessage = "jdbc:mysql://db.internal/forge?password=secret";
        ExcelImportServiceImpl service = new ExcelImportServiceImpl();

        ImportResult<GenericRowData> result = service.importData(
                new ByteArrayInputStream(internalMessage.getBytes(StandardCharsets.UTF_8)),
                internalMessage,
                GenericRowData.class);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getSummary())
                .isEqualTo(ImportResult.PUBLIC_FAILURE_MESSAGE)
                .doesNotContain(internalMessage);
    }

    @Test
    void listenerShouldNotExposeUnexpectedParserMessage() {
        String internalMessage = "Zip bomb detected at /private/upload.xlsx";
        ImportResult<GenericRowData> result = new ImportResult<>();
        GenericRowDataListener listener = new GenericRowDataListener(List.of(), result, null);

        listener.onException(new IllegalStateException(internalMessage), null);

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getErrorMessage())
                .isEqualTo("文件内容解析失败")
                .doesNotContain(internalMessage);
    }

    @Test
    void importResultSerializationShouldHideInternalErrorReportPath() throws Exception {
        String internalPath = "/private/import/error-report.xlsx";
        ImportResult<Object> result = new ImportResult<>();
        result.setErrorReportPath(internalPath);

        assertThat(new ObjectMapper().writeValueAsString(result))
                .doesNotContain("errorReportPath", internalPath);
    }
}
