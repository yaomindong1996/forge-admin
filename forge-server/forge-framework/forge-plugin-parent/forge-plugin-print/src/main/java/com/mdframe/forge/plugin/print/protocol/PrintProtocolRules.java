package com.mdframe.forge.plugin.print.protocol;

import static com.mdframe.forge.plugin.print.protocol.PrintProtocolLimits.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 每次校验独立实例，ID 与元素计数不跨请求共享。
 */
final class PrintProtocolRules {

    final List<PrintProtocolValidator.Issue> issues = new ArrayList<>();

    private final Set<String> ids = new HashSet<>();

    private int elements;

    private final PrintValueRules values = new PrintValueRules(this);

    private final PrintTableRules tables = new PrintTableRules(this, values);

    private final PrintStaticTableRules staticTables = new PrintStaticTableRules(this, values);

    void issue(String path, String code, String message) {
        if (issues.size() < MAX_ISSUES) {
            issues.add(new PrintProtocolValidator.Issue(path, code, message));
        }
    }

    boolean object(JsonNode value, String path, String... keys) {
        if (value == null || !value.isObject()) {
            issue(path, "INVALID_OBJECT", "必须是 JSON 对象");
            return false;
        }
        Set<String> allowed = Set.of(keys);
        value.fieldNames().forEachRemaining(key -> {
            if (!allowed.contains(key)) {
                issue(child(path, key), "UNKNOWN_PROPERTY", "不支持此属性");
            }
        });
        return true;
    }

    boolean array(JsonNode value, String path, int max) {
        if (value == null || !value.isArray() || value.size() > max) {
            issue(path, "INVALID_ARRAY", "数组项数超过限制或类型不正确");
            return false;
        }
        return true;
    }

    boolean number(JsonNode value, String path, double min, double max) {
        if (value == null || !value.isNumber() || !Double.isFinite(value.doubleValue()) || value.doubleValue() < min || value.doubleValue() > max) {
            issue(path, "INVALID_NUMBER", "数字类型或范围不正确");
            return false;
        }
        return true;
    }

    void integer(JsonNode value, String path, int min, int max) {
        if (number(value, path, min, max) && value.decimalValue().stripTrailingZeros().scale() > 0) {
            issue(path, "INVALID_NUMBER", "必须为整数");
        }
    }

    void choice(JsonNode value, String path, String... options) {
        if (value == null || !value.isTextual() || !Set.of(options).contains(value.textValue())) {
            issue(path, "UNSUPPORTED_VALUE", "不支持此配置值");
        }
    }

    void bool(JsonNode value, String path) {
        if (value == null || !value.isBoolean()) {
            issue(path, "UNSUPPORTED_VALUE", "必须是布尔值");
        }
    }

    void text(JsonNode value, String path, int max) {
        if (value == null || !value.isTextual() || value.textValue().length() > max) {
            issue(path, "INVALID_TEXT", "文本类型或长度不正确");
        }
    }

    void identifier(JsonNode value, String path) {
        if (value == null || !value.isTextual() || !value.textValue().matches("[A-Za-z0-9_-]{1,80}")) {
            issue(path, "INVALID_ID", "标识无效");
        } else if (!ids.add(value.textValue())) {
            issue(path, "DUPLICATE_ID", "标识重复");
        }
    }

    static String child(String path, String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    static double n(JsonNode value, String key) {
        return value.path(key).asDouble(Double.NaN);
    }

    void document(JsonNode doc) {
        if (!object(doc, "", "protocol", "schemaVersion", "paper", "header", "body", "footer", "resources", "watermark", "exportFileName")) {
            return;
        }
        choice(doc.get("protocol"), "protocol", "forge-print");
        JsonNode version = doc.get("schemaVersion");
        if (version == null || !version.isNumber() || version.decimalValue().compareTo(java.math.BigDecimal.valueOf(SCHEMA_VERSION)) != 0) {
            issue("schemaVersion", "UNSUPPORTED_VALUE", "仅支持协议版本 1");
        }
        JsonNode paper = doc.get("paper");
        if (!object(paper, "paper", "widthMm", "heightMm", "orientation", "marginMm", "kind", "tiling", "designBackground")) {
            return;
        }
        number(paper.get("widthMm"), "paper.widthMm", 10, PAPER_SIZE_MM);
        number(paper.get("heightMm"), "paper.heightMm", 10, PAPER_SIZE_MM);
        choice(paper.get("orientation"), "paper.orientation", "PORTRAIT", "LANDSCAPE");
        if (paper.has("kind")) {
            choice(paper.get("kind"), "paper.kind", "SHEET", "CONTINUOUS");
        }
        if (paper.has("tiling") && object(paper.get("tiling"), "paper.tiling", "enabled", "columns", "rows", "gapXMm", "gapYMm", "sheetWidthMm", "sheetHeightMm", "repeatToFill")) {
            JsonNode tiling = paper.get("tiling");
            if (tiling.has("enabled")) {
                bool(tiling.get("enabled"), "paper.tiling.enabled");
            }
            if (tiling.has("columns")) {
                integer(tiling.get("columns"), "paper.tiling.columns", 1, 12);
            }
            if (tiling.has("rows")) {
                integer(tiling.get("rows"), "paper.tiling.rows", 1, 20);
            }
            if (tiling.has("gapXMm")) {
                number(tiling.get("gapXMm"), "paper.tiling.gapXMm", 0, 50);
            }
            if (tiling.has("gapYMm")) {
                number(tiling.get("gapYMm"), "paper.tiling.gapYMm", 0, 50);
            }
            if (tiling.has("sheetWidthMm")) {
                number(tiling.get("sheetWidthMm"), "paper.tiling.sheetWidthMm", 10, PAPER_SIZE_MM);
            }
            if (tiling.has("sheetHeightMm")) {
                number(tiling.get("sheetHeightMm"), "paper.tiling.sheetHeightMm", 10, PAPER_SIZE_MM);
            }
            if (tiling.has("repeatToFill")) {
                bool(tiling.get("repeatToFill"), "paper.tiling.repeatToFill");
            }
        }
        if (paper.has("designBackground") && object(paper.get("designBackground"), "paper.designBackground", "fileId", "opacity", "rotationDeg", "print")) {
            JsonNode overlay = paper.get("designBackground");
            if (!PrintValueRules.fileId(overlay.get("fileId"))) {
                issue("paper.designBackground.fileId", "INVALID_RESOURCE", "套打底图必须使用文件标识");
            }
            if (overlay.has("opacity")) {
                number(overlay.get("opacity"), "paper.designBackground.opacity", 0, 1);
            }
            if (overlay.has("rotationDeg")) {
                number(overlay.get("rotationDeg"), "paper.designBackground.rotationDeg", -180, 180);
            }
            if (overlay.has("print")) {
                bool(overlay.get("print"), "paper.designBackground.print");
            }
        }
        if (doc.has("exportFileName")) {
            text(doc.get("exportFileName"), "exportFileName", 120);
            if (doc.get("exportFileName").isTextual()) {
                String name = doc.get("exportFileName").textValue();
                if (name.matches(".*[\\\\/\\p{Cntrl}].*")
                        || !name.replaceAll("\\{\\{[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*\\}\\}", "")
                                .replaceAll("\\{(timestamp|template)\\}", "")
                                .matches("[^{}]*")) {
                    issue("exportFileName", "INVALID_TEXT", "导出文件名占位符无效或包含路径分隔符");
                }
            }
        }
        if (doc.has("watermark") && object(doc.get("watermark"), "watermark", "text", "expression", "opacity", "rotateDeg", "gapXMm", "gapYMm", "fontSizePt", "color", "enabled")) {
            JsonNode watermark = doc.get("watermark");
            if (watermark.has("text")) {
                text(watermark.get("text"), "watermark.text", 100);
            }
            if (watermark.has("expression")) {
                PrintExpressionRules.check(watermark.get("expression"), "watermark.expression", this);
            }
            if (watermark.has("opacity")) {
                number(watermark.get("opacity"), "watermark.opacity", 0, 1);
            }
            if (watermark.has("rotateDeg")) {
                number(watermark.get("rotateDeg"), "watermark.rotateDeg", -180, 180);
            }
            if (watermark.has("gapXMm")) {
                number(watermark.get("gapXMm"), "watermark.gapXMm", 10, 200);
            }
            if (watermark.has("gapYMm")) {
                number(watermark.get("gapYMm"), "watermark.gapYMm", 10, 200);
            }
            if (watermark.has("fontSizePt")) {
                number(watermark.get("fontSizePt"), "watermark.fontSizePt", 6, 72);
            }
            if (watermark.has("color") && !PrintValueRules.isPrintColor(watermark.get("color"))) {
                issue("watermark.color", "INVALID_COLOR", "颜色须使用十六进制格式");
            }
            if (watermark.has("enabled")) {
                bool(watermark.get("enabled"), "watermark.enabled");
            }
        }
        JsonNode margins = paper.get("marginMm");
        if (!object(margins, "paper.marginMm", "top", "right", "bottom", "left")) {
            return;
        }
        for (String key : List.of("top", "right", "bottom", "left")) {
            number(margins.get(key), "paper.marginMm." + key, 0, PAPER_SIZE_MM);
        }
        boolean landscape = "LANDSCAPE".equals(paper.path("orientation").asText());
        double shortSide = Math.min(n(paper, "widthMm"), n(paper, "heightMm"));
        double longSide = Math.max(n(paper, "widthMm"), n(paper, "heightMm"));
        double width = (landscape ? longSide : shortSide) - n(margins, "left") - n(margins, "right");
        double height = (landscape ? shortSide : longSide) - n(margins, "top") - n(margins, "bottom");
        band(doc.get("header"), "header", width);
        band(doc.get("footer"), "footer", width);
        if (!(width > 0 && height - doc.path("header").path("heightMm").asDouble(Double.NaN) - doc.path("footer").path("heightMm").asDouble(Double.NaN) > 0)) {
            issue("paper", "NO_PRINTABLE_AREA", "页边距、页眉和页脚未留下正文区域");
        }
        if (array(doc.get("body"), "body", SECTIONS)) {
            for (int i = 0; i < doc.get("body").size(); i++) {
                JsonNode current = doc.get("body").get(i);
                String path = "body[" + i + "]";
                section(current, path, width);
                if ("PAGE_BREAK".equals(current.path("kind").asText())
                        && (i == 0 || i == doc.get("body").size() - 1 || "PAGE_BREAK".equals(doc.get("body").get(i - 1).path("kind").asText()))) {
                    issue(path, "INVALID_PAGE_BREAK", "分页符只能放在两个内容区块之间且不能连续");
                }
            }
        }
        if (elements > ELEMENTS) {
            issue("body", "TOO_MANY_ELEMENTS", "元素总数超过 1000");
        }
        if (array(doc.get("resources"), "resources", RESOURCES)) {
            for (int i = 0; i < doc.get("resources").size(); i++) {
                JsonNode resource = doc.get("resources").get(i);
                String path = "resources[" + i + "]";
                if (!object(resource, path, "id", "fileId")) {
                    continue;
                }
                identifier(resource.get("id"), path + ".id");
                if (!PrintValueRules.fileId(resource.get("fileId"))) {
                    issue(path, "INVALID_RESOURCE", "资源必须使用文件标识");
                }
            }
        }
    }

    private void band(JsonNode band, String path, double width) {
        if (!object(band, path, "heightMm", "repeat", "elements")) {
            return;
        }
        number(band.get("heightMm"), path + ".heightMm", 0, PAPER_SIZE_MM);
        bool(band.get("repeat"), path + ".repeat");
        elements(band.get("elements"), path + ".elements", width, n(band, "heightMm"));
    }

    private void elements(JsonNode list, String path, double width, double height) {
        if (!array(list, path, ELEMENTS)) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            element(list.get(i), path + "[" + i + "]", width, height);
        }
    }

    private void element(JsonNode e, String path, double width, double height) {
        if (!object(e, path, "id", "type", "xMm", "yMm", "widthMm", "heightMm", "binding", "format", "style", "table", "barcodeFormat", "pageNumberFormat", "showCodeText", "rotationDeg", "flipX", "flipY", "locked", "collectionPath", "columns", "headerRows", "repeatHeader", "footer", "subtotal", "emptyText", "headerStyle", "oddRowStyle", "evenRowStyle", "minHeightMm", "cellStyles", "descriptions")) {
            return;
        }
        elements++;
        identifier(e.get("id"), path + ".id");
        choice(e.get("type"), path + ".type", "TEXT", "IMAGE", "HTML", "LINE", "RECTANGLE", "ELLIPSE", "BARCODE", "QRCODE", "PAGE_NUMBER", "STATIC_TABLE", "DATA_TABLE", "DESCRIPTIONS");
        for (String key : List.of("xMm", "yMm", "widthMm", "heightMm")) {
            number(e.get(key), path + "." + key, (key.equals("widthMm") || key.equals("heightMm")) ? .1 : 0, PAPER_SIZE_MM);
        }
        if (n(e, "xMm") + n(e, "widthMm") > width + GEOMETRY_TOLERANCE_MM || n(e, "yMm") + n(e, "heightMm") > height + GEOMETRY_TOLERANCE_MM) {
            issue(path, "OUT_OF_BOUNDS", "元素超出所属区块");
        }
        String type = e.path("type").asText();
        if (e.has("binding") || Set.of("TEXT", "IMAGE", "HTML", "BARCODE", "QRCODE").contains(type)) {
            values.binding(e.get("binding"), path + ".binding", type.equals("IMAGE"), type.equals("TEXT") || type.equals("HTML"));
        }
        if (e.has("barcodeFormat")) {
            choice(e.get("barcodeFormat"), path + ".barcodeFormat", "CODE128", "CODE39", "EAN13", "EAN8", "ITF14");
        }
        if (e.has("pageNumberFormat")) {
            choice(e.get("pageNumberFormat"), path + ".pageNumberFormat", "CURRENT", "CURRENT_TOTAL");
        }
        if (e.has("showCodeText")) {
            bool(e.get("showCodeText"), path + ".showCodeText");
        }
        if (type.equals("STATIC_TABLE") || e.has("table")) {
            staticTables.table(e.get("table"), path + ".table", n(e, "widthMm"), n(e, "heightMm"));
        }
        if (type.equals("DATA_TABLE") || List.of("collectionPath", "columns", "headerRows", "repeatHeader", "footer", "subtotal", "emptyText").stream().anyMatch(e::has)) {
            tables.table(e, path, n(e, "widthMm"));
        }
        if (type.equals("DESCRIPTIONS") || e.has("descriptions")) {
            descriptions(e.get("descriptions"), path + ".descriptions");
        }
        if (e.has("rotationDeg")) {
            number(e.get("rotationDeg"), path + ".rotationDeg", -180, 180);
        }
        for (String key : List.of("flipX", "flipY", "locked")) {
            if (e.has(key)) {
                bool(e.get(key), path + "." + key);
            }
        }
        values.style(e.get("style"), path + ".style");
        if (e.has("headerStyle")) {
            values.style(e.get("headerStyle"), path + ".headerStyle");
        }
        if (e.has("oddRowStyle")) {
            values.style(e.get("oddRowStyle"), path + ".oddRowStyle");
        }
        if (e.has("evenRowStyle")) {
            values.style(e.get("evenRowStyle"), path + ".evenRowStyle");
        }
        if (e.has("minHeightMm")) {
            number(e.get("minHeightMm"), path + ".minHeightMm", 0, PAPER_SIZE_MM);
        }
        if (e.has("cellStyles")) {
            values.cellStyles(e.get("cellStyles"), path + ".cellStyles");
        }
        values.format(e.get("format"), path + ".format");
    }

    private void descriptions(JsonNode block, String path) {
        if (!object(block, path, "bordered", "column", "labelAlign", "labelPlacement", "labelBackground", "separator", "size", "title", "titleFontSizePt", "titleColor", "titleAlign", "titleBold", "items")) {
            return;
        }
        if (block.has("bordered")) {
            bool(block.get("bordered"), path + ".bordered");
        }
        int column = 3;
        if (block.has("column")) {
            integer(block.get("column"), path + ".column", 1, 4);
            if (block.get("column").isNumber()) {
                column = block.get("column").intValue();
            }
        }
        if (block.has("labelAlign")) {
            choice(block.get("labelAlign"), path + ".labelAlign", "left", "center", "right");
        }
        if (block.has("labelPlacement")) {
            choice(block.get("labelPlacement"), path + ".labelPlacement", "top", "left");
        }
        if (block.has("labelBackground") && !PrintValueRules.isPrintColor(block.get("labelBackground"))) {
            issue(path + ".labelBackground", "INVALID_COLOR", "颜色须使用十六进制格式");
        }
        if (block.has("separator")) {
            text(block.get("separator"), path + ".separator", 8);
        }
        if (block.has("size")) {
            choice(block.get("size"), path + ".size", "small", "medium", "large");
        }
        if (block.has("title")) {
            text(block.get("title"), path + ".title", 80);
        }
        if (block.has("titleFontSizePt")) {
            number(block.get("titleFontSizePt"), path + ".titleFontSizePt", 6, 72);
        }
        if (block.has("titleColor") && !PrintValueRules.isPrintColor(block.get("titleColor"))) {
            issue(path + ".titleColor", "INVALID_COLOR", "颜色须使用十六进制格式");
        }
        if (block.has("titleAlign")) {
            choice(block.get("titleAlign"), path + ".titleAlign", "left", "center", "right");
        }
        if (block.has("titleBold")) {
            bool(block.get("titleBold"), path + ".titleBold");
        }
        if (!array(block.get("items"), path + ".items", 40)) {
            return;
        }
        int index = 0;
        for (JsonNode item : block.get("items")) {
            String location = path + ".items[" + index + "]";
            if (object(item, location, "id", "label", "span", "binding")) {
                identifier(item.get("id"), location + ".id");
                text(item.get("label"), location + ".label", 40);
                int span = 1;
                if (item.has("span")) {
                    integer(item.get("span"), location + ".span", 1, 4);
                    if (item.get("span").isNumber()) {
                        span = item.get("span").intValue();
                    }
                }
                if (span > column) {
                    issue(location + ".span", "INVALID_SPAN", "跨列不能超过总列数");
                }
                values.binding(item.get("binding"), location + ".binding", false, false);
            }
            index++;
        }
    }

    private void section(JsonNode s, String path, double width) {
        if (!object(s, path, "id", "kind", "heightMm", "elements", "binding", "format", "style", "gapAfterMm", "keepWithNext", "collectionPath", "columns", "headerRows", "repeatHeader", "footer", "subtotal", "emptyText", "minHeightMm", "headerStyle", "oddRowStyle", "evenRowStyle", "cellStyles")) {
            return;
        }
        identifier(s.get("id"), path + ".id");
        choice(s.get("kind"), path + ".kind", "FIXED", "TEXT", "TABLE", "PAGE_BREAK");
        String kind = s.path("kind").asText();
        if (kind.equals("PAGE_BREAK")) {
            s.fieldNames().forEachRemaining(key -> {
                if (!Set.of("id", "kind").contains(key)) {
                    issue(child(path, key), "UNKNOWN_PROPERTY", "分页符不支持此属性");
                }
            });
            return;
        }
        if (s.has("gapAfterMm")) {
            number(s.get("gapAfterMm"), path + ".gapAfterMm", 0, 100);
        }
        if (s.has("keepWithNext")) {
            bool(s.get("keepWithNext"), path + ".keepWithNext");
        }
        values.style(s.get("style"), path + ".style");
        if (s.has("headerStyle")) {
            values.style(s.get("headerStyle"), path + ".headerStyle");
        }
        if (s.has("oddRowStyle")) {
            values.style(s.get("oddRowStyle"), path + ".oddRowStyle");
        }
        if (s.has("evenRowStyle")) {
            values.style(s.get("evenRowStyle"), path + ".evenRowStyle");
        }
        if (s.has("cellStyles")) {
            values.cellStyles(s.get("cellStyles"), path + ".cellStyles");
        }
        if (s.has("minHeightMm")) {
            number(s.get("minHeightMm"), path + ".minHeightMm", 0, PAPER_SIZE_MM);
        }
        values.format(s.get("format"), path + ".format");
        if (kind.equals("FIXED") || s.has("heightMm")) {
            number(s.get("heightMm"), path + ".heightMm", .1, PAPER_SIZE_MM);
        }
        if (kind.equals("FIXED") || s.has("elements")) {
            elements(s.get("elements"), path + ".elements", width, n(s, "heightMm"));
        }
        if (kind.equals("TEXT") || s.has("binding")) {
            values.binding(s.get("binding"), path + ".binding", false, false);
        }
        if (kind.equals("TABLE") || List.of("columns", "collectionPath", "headerRows", "repeatHeader", "footer", "subtotal", "emptyText").stream().anyMatch(s::has)) {
            tables.table(s, path, width);
        }
    }
}
