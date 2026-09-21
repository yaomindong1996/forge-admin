package com.mdframe.forge.plugin.print.protocol;

import java.util.List;
import com.mdframe.forge.plugin.print.protocol.PrintElement.Binding;
import com.mdframe.forge.plugin.print.protocol.PrintElement.Format;
import com.mdframe.forge.plugin.print.protocol.PrintElement.Style;

/**
 * 固定区块、流式文本、明细表格与手动分页符的明确协议模型。
 */
public record PrintSection(String id, String kind, Double heightMm, List<PrintElement> elements, Binding binding, Format format, Style style, Double gapAfterMm, Boolean keepWithNext, String collectionPath, List<Column> columns, List<HeaderRow> headerRows, Boolean repeatHeader, Footer footer, Footer subtotal, String emptyText, Double minHeightMm, Style headerStyle, Style oddRowStyle, Style evenRowStyle, java.util.Map<String, Style> cellStyles) {

    public record Column(String id, String field, String title, Double widthMm, Format format, Style style, Style headerStyle) {
    }

    public record HeaderRow(List<HeaderCell> cells) {
    }

    public record HeaderCell(String text, Integer span, Style style, String contentType, Binding binding, Format format) {
    }

    public record Footer(List<FooterCell> cells) {
    }

    public record FooterCell(Binding binding, Integer span, Format format, Style style, String contentType) {
    }
}
