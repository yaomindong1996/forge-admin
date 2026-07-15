package com.mdframe.forge.starter.excel.core;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.mdframe.forge.starter.excel.model.ImportTemplateColumn;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一导入模板写入器：首个工作表提供可替换的样例行，第二个工作表提供字段填写说明。
 */
public final class ExcelImportTemplateWriter {

    private static final String DEFAULT_DATA_SHEET_NAME = "导入数据";
    private static final String INSTRUCTION_SHEET_NAME = "填写说明";

    private ExcelImportTemplateWriter() {
    }

    public static byte[] write(String dataSheetName, List<ImportTemplateColumn> columns) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        write(outputStream, dataSheetName, columns);
        return outputStream.toByteArray();
    }

    public static void write(OutputStream outputStream,
                             String dataSheetName,
                             List<ImportTemplateColumn> columns) {
        if (outputStream == null) {
            throw new IllegalArgumentException("Excel 输出流不能为空");
        }
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("导入模板字段不能为空");
        }

        ExcelWriter excelWriter = EasyExcel.write(outputStream).build();
        try {
            WriteSheet dataSheet = EasyExcel.writerSheet(normalizeSheetName(dataSheetName))
                    .head(buildDataHeaders(columns))
                    .build();
            excelWriter.write(List.of(buildSampleRow(columns)), dataSheet);

            WriteSheet instructionSheet = EasyExcel.writerSheet(INSTRUCTION_SHEET_NAME)
                    .head(buildInstructionHeaders())
                    .build();
            excelWriter.write(buildInstructionRows(columns), instructionSheet);
        } finally {
            excelWriter.finish();
        }
    }

    private static String normalizeSheetName(String dataSheetName) {
        return dataSheetName == null || dataSheetName.isBlank()
                ? DEFAULT_DATA_SHEET_NAME
                : dataSheetName;
    }

    private static List<List<String>> buildDataHeaders(List<ImportTemplateColumn> columns) {
        return columns.stream()
                .map(column -> List.of(normalizeText(column.columnName(), column.fieldName())))
                .toList();
    }

    private static List<Object> buildSampleRow(List<ImportTemplateColumn> columns) {
        List<Object> row = new ArrayList<>(columns.size());
        for (ImportTemplateColumn column : columns) {
            row.add(normalizeText(column.exampleValue(), "示例值"));
        }
        return row;
    }

    private static List<List<String>> buildInstructionHeaders() {
        return List.of(
                List.of("序号"),
                List.of("字段名称"),
                List.of("字段编码"),
                List.of("是否必填"),
                List.of("样例值"),
                List.of("填写说明")
        );
    }

    private static List<List<Object>> buildInstructionRows(List<ImportTemplateColumn> columns) {
        List<List<Object>> rows = new ArrayList<>(columns.size());
        for (int index = 0; index < columns.size(); index++) {
            ImportTemplateColumn column = columns.get(index);
            rows.add(List.of(
                    index + 1,
                    normalizeText(column.columnName(), column.fieldName()),
                    normalizeText(column.fieldName(), "-"),
                    column.required() ? "是" : "否",
                    normalizeText(column.exampleValue(), "示例值"),
                    normalizeDescription(column)
            ));
        }
        return rows;
    }

    private static String normalizeDescription(ImportTemplateColumn column) {
        String description = normalizeText(column.description(), "按字段业务含义填写");
        String prefix = column.required() ? "必填。" : "选填。";
        String sampleReminder = "导入前请删除或替换模板中的样例行。";
        return prefix + description + " " + sampleReminder;
    }

    private static String normalizeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
