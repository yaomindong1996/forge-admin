package com.mdframe.forge.plugin.print.protocol;

import static com.mdframe.forge.plugin.print.protocol.PrintProtocolLimits.*;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 明细列宽和表头/合计跨度规则。
 */
final class PrintTableRules {

    private final PrintProtocolRules r;

    private final PrintValueRules values;

    PrintTableRules(PrintProtocolRules rules, PrintValueRules values) {
        this.r = rules;
        this.values = values;
    }

    void table(JsonNode t, String path, double width) {
        if (!PrintValueRules.field(t.get("collectionPath"))) {
            r.issue(path + ".collectionPath", "INVALID_FIELD_PATH", "明细路径无效");
        }
        r.bool(t.get("repeatHeader"), path + ".repeatHeader");
        JsonNode columns = t.get("columns");
        if (!r.array(columns, path + ".columns", COLUMNS)) {
            return;
        }
        if (columns.isEmpty()) {
            r.issue(path, "EMPTY_COLUMNS", "表格至少需要一列");
        }
        double totalWidth = 0;
        for (int i = 0; i < columns.size(); i++) {
            JsonNode c = columns.get(i);
            String at = path + ".columns[" + i + "]";
            if (!r.object(c, at, "id", "field", "title", "widthMm", "format", "style", "headerStyle")) {
                continue;
            }
            r.identifier(c.get("id"), at + ".id");
            if (!PrintValueRules.field(c.get("field"))) {
                r.issue(at + ".field", "INVALID_FIELD_PATH", "明细字段无效");
            }
            r.text(c.get("title"), at + ".title", 200);
            if (r.number(c.get("widthMm"), at + ".widthMm", 1, PAPER_SIZE_MM)) {
                totalWidth += c.get("widthMm").doubleValue();
            }
            values.format(c.get("format"), at + ".format");
            values.style(c.get("style"), at + ".style");
            if (c.has("headerStyle")) {
                values.style(c.get("headerStyle"), at + ".headerStyle");
            }
        }
        if (totalWidth > width + GEOMETRY_TOLERANCE_MM) {
            r.issue(path, "OUT_OF_BOUNDS", "表格总列宽超出纸张正文");
        }
        if (t.has("headerRows") && r.array(t.get("headerRows"), path + ".headerRows", HEADER_ROWS)) {
            for (int i = 0; i < t.get("headerRows").size(); i++) {
                cells(t.get("headerRows").get(i), path + ".headerRows[" + i + "]", columns.size(), false);
            }
        }
        if (t.has("footer")) {
            cells(t.get("footer"), path + ".footer", columns.size(), true);
        }
        if (t.has("subtotal")) {
            cells(t.get("subtotal"), path + ".subtotal", columns.size(), true);
        }
        if (t.has("emptyText")) {
            r.text(t.get("emptyText"), path + ".emptyText", 500);
        }
    }

    private void cells(JsonNode row, String path, int count, boolean footer) {
        if (!r.object(row, path, "cells") || !r.array(row.get("cells"), path + ".cells", COLUMNS)) {
            return;
        }
        double total = 0;
        for (int i = 0; i < row.get("cells").size(); i++) {
            JsonNode c = row.get("cells").get(i);
            String at = path + ".cells[" + i + "]";
            if (c.has("contentType")) {
                r.choice(c.get("contentType"), at + ".contentType", "TEXT", "IMAGE");
            }
            boolean image = "IMAGE".equals(c.path("contentType").asText());
            String[] keys = (footer || image)
                    ? new String[] { "binding", "span", "format", "style", "contentType" }
                    : new String[] { "text", "span", "style", "contentType" };
            if (!r.object(c, at, keys)) {
                continue;
            }
            JsonNode span = c.get("span");
            if (span == null || !span.isNumber() || span.doubleValue() < 1 || span.doubleValue() > count || span.decimalValue().stripTrailingZeros().scale() > 0) {
                r.issue(at, "INVALID_SPAN", "单元格跨度无效");
            }
            total += c.path("span").asDouble(Double.NaN);
            if (footer || image) {
                values.binding(c.get("binding"), at + ".binding", image, false);
                values.format(c.get("format"), at + ".format");
            } else {
                r.text(c.get("text"), at + ".text", 500);
            }
            values.style(c.get("style"), at + ".style");
        }
        if (total != count) {
            r.issue(path, "INVALID_SPAN", "表头或表尾跨度必须覆盖全部列");
        }
    }
}
