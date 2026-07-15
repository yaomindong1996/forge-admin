package com.mdframe.forge.starter.excel.core;

import com.mdframe.forge.starter.excel.model.ImportTemplateColumn;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelImportTemplateWriterTest {

    @Test
    void shouldWriteSampleDataAndFieldInstructions() throws Exception {
        byte[] workbookBytes = ExcelImportTemplateWriter.write(
                "导入数据",
                List.of(
                        new ImportTemplateColumn("username", "用户名", true, "zhangsan", "账号唯一"),
                        new ImportTemplateColumn("status", "状态", false, "启用", "填写启用或停用")
                )
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            assertEquals(2, workbook.getNumberOfSheets());
            assertNotNull(workbook.getSheet("导入数据"));
            assertNotNull(workbook.getSheet("填写说明"));
            assertEquals("用户名", workbook.getSheet("导入数据").getRow(0).getCell(0).getStringCellValue());
            assertEquals("zhangsan", workbook.getSheet("导入数据").getRow(1).getCell(0).getStringCellValue());
            assertEquals("字段编码", workbook.getSheet("填写说明").getRow(0).getCell(2).getStringCellValue());
            assertEquals("username", workbook.getSheet("填写说明").getRow(1).getCell(2).getStringCellValue());
            assertTrue(workbook.getSheet("填写说明").getRow(1).getCell(5).getStringCellValue()
                    .contains("必填。账号唯一"));
        }
    }
}
