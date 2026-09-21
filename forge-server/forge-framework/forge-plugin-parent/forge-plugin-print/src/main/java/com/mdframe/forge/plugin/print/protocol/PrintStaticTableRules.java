package com.mdframe.forge.plugin.print.protocol;

import static com.mdframe.forge.plugin.print.protocol.PrintProtocolLimits.*;
import com.fasterxml.jackson.databind.JsonNode;

/** 原生空白表格覆盖矩阵校验；模板只保存声明式单元格，不接受 HTML。 */
final class PrintStaticTableRules {

    private final PrintProtocolRules r;

    private final PrintValueRules values;

    PrintStaticTableRules(PrintProtocolRules rules, PrintValueRules valueRules) {
        this.r = rules;
        this.values = valueRules;
    }

    void table(JsonNode table, String path, double widthMm, double heightMm) {
        if (!r.object(table, path, "columns", "rows", "cells")) {
            return;
        }
        JsonNode columns = table.get("columns");
        JsonNode rows = table.get("rows");
        JsonNode cells = table.get("cells");
        if (!r.array(columns, path + ".columns", STATIC_TABLE_COLUMNS)
                || !r.array(rows, path + ".rows", STATIC_TABLE_ROWS)
                || !r.array(cells, path + ".cells", STATIC_TABLE_COLUMNS * STATIC_TABLE_ROWS)) {
            return;
        }
        if (columns.isEmpty() || rows.isEmpty()) {
            r.issue(path, "EMPTY_TABLE", "空白表格至少需要一行一列");
            return;
        }
        double totalWidth = 0;
        for (int i = 0; i < columns.size(); i++) {
            JsonNode column = columns.get(i);
            String location = path + ".columns[" + i + "]";
            if (!r.object(column, location, "id", "widthMm")) {
                continue;
            }
            r.identifier(column.get("id"), location + ".id");
            if (r.number(column.get("widthMm"), location + ".widthMm", .1, PAPER_SIZE_MM)) {
                totalWidth += column.get("widthMm").doubleValue();
            }
        }
        double totalHeight = 0;
        for (int i = 0; i < rows.size(); i++) {
            JsonNode row = rows.get(i);
            String location = path + ".rows[" + i + "]";
            if (!r.object(row, location, "id", "heightMm")) {
                continue;
            }
            r.identifier(row.get("id"), location + ".id");
            if (r.number(row.get("heightMm"), location + ".heightMm", .1, PAPER_SIZE_MM)) {
                totalHeight += row.get("heightMm").doubleValue();
            }
        }
        if (Math.abs(totalWidth - widthMm) > GEOMETRY_TOLERANCE_MM || Math.abs(totalHeight - heightMm) > GEOMETRY_TOLERANCE_MM) {
            r.issue(path, "TABLE_SIZE_MISMATCH", "表格行列尺寸必须与元素尺寸一致");
        }
        int[][] coverage = new int[rows.size()][columns.size()];
        for (int i = 0; i < cells.size(); i++) {
            validateCell(cells.get(i), path + ".cells[" + i + "]", rows.size(), columns.size(), coverage);
        }
        for (int[] row : coverage) {
            for (int count : row) {
                if (count != 1) {
                    r.issue(path, "INVALID_COVERAGE", "单元格必须完整覆盖表格且不能重叠");
                    return;
                }
            }
        }
    }

    private void validateCell(JsonNode cell, String path, int rowCount, int columnCount, int[][] coverage) {
        if (!r.object(cell, path, "id", "row", "column", "rowSpan", "colSpan", "binding", "format", "style", "contentType", "imageWidthMm", "imageHeightMm")) {
            return;
        }
        r.identifier(cell.get("id"), path + ".id");
        r.integer(cell.get("row"), path + ".row", 0, rowCount);
        r.integer(cell.get("column"), path + ".column", 0, columnCount);
        r.integer(cell.get("rowSpan"), path + ".rowSpan", 1, rowCount);
        r.integer(cell.get("colSpan"), path + ".colSpan", 1, columnCount);
        if (cell.has("contentType")) {
            r.choice(cell.get("contentType"), path + ".contentType", "TEXT", "IMAGE");
        }
        if (cell.has("imageWidthMm")) {
            r.number(cell.get("imageWidthMm"), path + ".imageWidthMm", 1, 500);
        }
        if (cell.has("imageHeightMm")) {
            r.number(cell.get("imageHeightMm"), path + ".imageHeightMm", 1, 500);
        }
        boolean image = "IMAGE".equals(cell.path("contentType").asText());
        values.binding(cell.get("binding"), path + ".binding", image, false);
        values.format(cell.get("format"), path + ".format");
        values.style(cell.get("style"), path + ".style");
        int row = cell.path("row").asInt(-1);
        int column = cell.path("column").asInt(-1);
        int rowSpan = cell.path("rowSpan").asInt(-1);
        int colSpan = cell.path("colSpan").asInt(-1);
        if (row < 0 || column < 0 || rowSpan < 1 || colSpan < 1 || row + rowSpan > rowCount || column + colSpan > columnCount) {
            r.issue(path, "INVALID_SPAN", "单元格超出表格范围");
            return;
        }
        for (int y = row; y < row + rowSpan; y++) {
            for (int x = column; x < column + colSpan; x++) {
                coverage[y][x]++;
            }
        }
    }
}
