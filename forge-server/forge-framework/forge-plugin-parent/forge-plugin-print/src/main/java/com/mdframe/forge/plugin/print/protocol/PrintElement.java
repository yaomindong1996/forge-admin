package com.mdframe.forge.plugin.print.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

/**
 * 可打印元素；常量保留 JSON 标量类型，不执行表达式。
 */
public record PrintElement(
        String id,
        String type,
        Double xMm,
        Double yMm,
        Double widthMm,
        Double heightMm,
        Binding binding,
        Format format,
        Style style,
        StaticTable table,
        String barcodeFormat,
        String pageNumberFormat,
        Boolean showCodeText,
        Double rotationDeg,
        Boolean flipX,
        Boolean flipY,
        Boolean locked,
        String collectionPath,
        List<PrintSection.Column> columns,
        List<PrintSection.HeaderRow> headerRows,
        Boolean repeatHeader,
        PrintSection.Footer footer,
        PrintSection.Footer subtotal,
        String emptyText,
        Style headerStyle,
        Style oddRowStyle,
        Style evenRowStyle,
        Double minHeightMm,
        Map<String, Style> cellStyles
) {

    public record Binding(String source, String path, JsonNode value, String expression) {
    }

    public record Format(String type, Integer scale, String emptyText, String trueText, String falseText, String datePattern) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Style(String fontFamily, Double fontSizePt, Integer fontWeight, String fontStyle, String textAlign, String verticalAlign, Double lineHeight, String color, String backgroundColor, String borderColor, Double borderWidthMm, String borderStyle, Double borderRadiusMm, Double paddingMm, String textDecoration, String objectFit, String textFit, Double shrinkMinFontSizePt, Double opacity) {
    }

    public record StaticTable(List<StaticColumn> columns, List<StaticRow> rows, List<StaticCell> cells) {
    }

    public record StaticColumn(String id, Double widthMm) {
    }

    public record StaticRow(String id, Double heightMm) {
    }

    public record StaticCell(String id, Integer row, Integer column, Integer rowSpan, Integer colSpan, Binding binding, Format format, Style style, String contentType, Double imageWidthMm, Double imageHeightMm) {
    }
}
