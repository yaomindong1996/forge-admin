package com.mdframe.forge.plugin.print;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class PrintProtocolValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private final PrintProtocolValidator validator = new PrintProtocolValidator();

    private ObjectNode document() throws Exception {
        try (var in = getClass().getResourceAsStream("/print/valid-document.json")) {
            return (ObjectNode) mapper.readTree(in);
        }
    }

    private void rejects(JsonNode doc, String path, String code) {
        assertThatThrownBy(() -> validator.validate(doc.toString())).isInstanceOfSatisfying(PrintProtocolValidator.InvalidTemplateException.class, ex -> assertThat(ex.getIssues()).anySatisfy(issue -> {
            assertThat(issue.path()).startsWith(path);
            assertThat(issue.code()).isEqualTo(code);
        }));
    }

    @Test
    void acceptsCompleteTemplateAndReturnsStableCanonicalHash() throws Exception {
        var original = validator.validate(document().toPrettyString());
        var again = validator.validate(original.canonicalJson());
        assertThat(original.schemaHash()).matches("[a-f0-9]{64}").isEqualTo(again.schemaHash());
        assertThat(original.document().body()).hasSize(3);
        assertThat(original.document().header().elements().get(0).binding().value().asText()).isEqualTo("合成采购单");
        var reversed = mapper.createObjectNode();
        var doc = document();
        java.util.List<String> keys = new java.util.ArrayList<>();
        doc.fieldNames().forEachRemaining(keys::add);
        java.util.Collections.reverse(keys);
        keys.forEach(key -> reversed.set(key, doc.get(key)));
        ((ObjectNode) reversed.get("paper")).put("widthMm", 210.0);
        assertThat(validator.validate(reversed.toString()).schemaHash()).isEqualTo(original.schemaHash());
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "false", "null", "9007199254740993", "0.000000000000000001" })
    void preservesLiteralValuesWithoutCoercion(String literal) throws Exception {
        var doc = document();
        ((ObjectNode) doc.at("/header/elements/0/binding")).set("value", mapper.readTree(literal));
        var result = validator.validate(doc.toString());
        assertThat(mapper.readTree(result.canonicalJson()).at("/header/elements/0/binding/value")).isEqualTo(mapper.readTree(literal));
    }

    @ParameterizedTest
    @ValueSource(strings = { "onclick", "html", "formatter", "__proto__", "constructor" })
    void rejectsUnknownAndExecutableProperties(String key) throws Exception {
        var doc = document();
        ((ObjectNode) doc.at("/body/0/elements/0")).put(key, "alert(1)");
        assertThatThrownBy(() -> validator.validate(doc.toString())).isInstanceOf(PrintProtocolValidator.InvalidTemplateException.class);
    }

    @Test
    void rejectsDuplicateKeysTrailingJsonAndExcessiveNesting() throws Exception {
        String valid = document().toString();
        for (String invalid : new String[] { valid.replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"schemaVersion\":1"), valid + "{}", "[".repeat(70) + "0" + "]".repeat(70) }) {
            assertThatThrownBy(() -> validator.validate(invalid)).isInstanceOf(PrintProtocolValidator.InvalidTemplateException.class);
        }
    }

    @Test
    void rejectsFutureProtocolAndWrongPrimitiveTypes() throws Exception {
        var doc = document();
        doc.put("schemaVersion", 2);
        rejects(doc, "schemaVersion", "UNSUPPORTED_VALUE");
        doc = document();
        ((ObjectNode) doc.get("paper")).put("widthMm", "210");
        rejects(doc, "paper.widthMm", "INVALID_NUMBER");
        doc = document();
        ((ObjectNode) doc.at("/body/0/elements/0")).putNull("style");
        rejects(doc, "body[0].elements[0].style", "INVALID_OBJECT");
    }

    @ParameterizedTest
    @ValueSource(strings = { "main.__proto__.secret", "constructor.name", "main.items[0]", "system.generatedAt", "https://example.com" })
    void rejectsUnsafeFieldBindings(String path) throws Exception {
        var doc = document();
        ((ObjectNode) doc.at("/body/0/elements/0/binding")).put("path", path);
        rejects(doc, "body[0].elements[0].binding", "INVALID_FIELD_PATH");
    }

    @Test
    void validatesBindingConflictsAndPageNumberContext() throws Exception {
        var doc = document();
        ((ObjectNode) doc.at("/body/0/elements/0/binding")).put("value", false);
        rejects(doc, "body[0].elements[0].binding", "CONFLICTING_BINDING");
        doc = document();
        ((ObjectNode) doc.at("/body/1/binding")).put("source", "SYSTEM").put("path", "system.pageNumber");
        rejects(doc, "body[1].binding", "INVALID_PAGE_NUMBER_BINDING");
        assertThatCode(() -> validator.validate(docTextWithFixedPage())).doesNotThrowAnyException();
    }

    private String docTextWithFixedPage() throws Exception {
        var doc = document();
        ((ObjectNode) doc.at("/body/0/elements/0/binding")).put("source", "SYSTEM").put("path", "system.pageNumber");
        return doc.toString();
    }

    @ParameterizedTest
    @ValueSource(strings = { "https://example.com/image.png", "javascript:alert(1)", "data:image/svg+xml;base64,PHN2Zz4=", "file:///tmp/private.png" })
    void rejectsUncontrolledImageSources(String value) throws Exception {
        var doc = document();
        ((ObjectNode) doc.at("/header/elements/0")).put("type", "IMAGE");
        ((ObjectNode) doc.at("/header/elements/0/binding")).put("value", value);
        rejects(doc, "header.elements[0].binding", "INVALID_RESOURCE");
    }

    @Test
    void rejectsDuplicateIdsAndGeometryOverflow() throws Exception {
        var doc = document();
        ((ObjectNode) doc.at("/resources/0")).put("id", "title");
        rejects(doc, "resources[0].id", "DUPLICATE_ID");
        doc = document();
        ((ObjectNode) doc.at("/body/0/elements/0")).put("xMm", 100);
        rejects(doc, "body[0].elements[0]", "OUT_OF_BOUNDS");
        doc = document();
        ((ObjectNode) doc.at("/paper/marginMm")).put("left", 210);
        rejects(doc, "paper", "NO_PRINTABLE_AREA");
    }

    @Test
    void rejectsInvalidTableWidthSpansAndEmptyColumns() throws Exception {
        var doc = document();
        ((ObjectNode) doc.at("/body/2/columns/0")).put("widthMm", 180);
        rejects(doc, "body[2]", "OUT_OF_BOUNDS");
        doc = document();
        ((ObjectNode) doc.at("/body/2/headerRows/0/cells/0")).put("span", 1);
        rejects(doc, "body[2].headerRows[0]", "INVALID_SPAN");
        doc = document();
        ((ObjectNode) doc.at("/body/2")).putArray("columns");
        rejects(doc, "body[2]", "EMPTY_COLUMNS");
    }

    @Test
    void rejectsResourceAndDocumentLimits() throws Exception {
        var doc = document();
        ((ObjectNode) doc.at("/header/elements/0/binding")).put("value", "字".repeat(100001));
        rejects(doc, "header.elements[0].binding", "TEXT_TOO_LONG");
        assertThatThrownBy(() -> validator.validate(" ".repeat(1024 * 1024 + 1))).isInstanceOf(PrintProtocolValidator.InvalidTemplateException.class);
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(PrintProtocolValidator.InvalidTemplateException.class);
        doc = document();
        var body = ((ObjectNode) doc).putArray("body");
        for (int i = 0; i < 201; i++) {
            body.addObject().put("id", "s" + i).put("kind", "FIXED").put("heightMm", 1).putArray("elements");
        }
        rejects(doc, "body", "INVALID_ARRAY");
    }

    @ParameterizedTest
    @ValueSource(strings = { "LINE", "RECTANGLE", "ELLIPSE", "BARCODE", "QRCODE", "IMAGE" })
    void acceptsEveryNativeElementWithoutExternalContent(String type) throws Exception {
        var doc = document();
        var element = (ObjectNode) doc.at("/header/elements/0");
        element.put("type", type);
        ((ObjectNode) element.get("binding")).put("value", type.equals("IMAGE") ? "synthetic_file_1" : "12345678");
        if (type.equals("BARCODE")) {
            element.put("barcodeFormat", "CODE128");
        }
        assertThat(validator.validate(doc.toString()).document().header().elements().get(0).type()).isEqualTo(type);
    }

    @Test
    void rejectsInvalidStylesFractionsAndUnusedUnsafeProperties() throws Exception {
        var doc = document();
        ((ObjectNode) doc.at("/body/0/elements/0/style")).put("fontFamily", "url(example)");
        rejects(doc, "body[0].elements[0].style", "INVALID_FONT");
        doc = document();
        ((ObjectNode) doc.at("/body/2/columns/1/format")).put("scale", 1.5);
        rejects(doc, "body[2].columns[1].format.scale", "INVALID_NUMBER");
        doc = document();
        ((ObjectNode) doc.at("/body/0/elements/0")).put("type", "LINE");
        ((ObjectNode) doc.at("/body/0/elements/0/binding")).put("path", "constructor.secret");
        rejects(doc, "body[0].elements[0].binding", "INVALID_FIELD_PATH");
        doc = document();
        ((ObjectNode) doc.at("/body/0/elements/0")).put("widthMm", 0);
        rejects(doc, "body[0].elements[0].widthMm", "INVALID_NUMBER");
    }

    @Test
    void validatesNativeTransformsLockingAndVisualOptions() throws Exception {
        var doc = document();
        var element = (ObjectNode) doc.at("/body/0/elements/0");
        element.put("rotationDeg", -90).put("flipX", true).put("flipY", false).put("locked", true);
        ((ObjectNode) element.get("style")).put("borderStyle", "dashed").put("borderRadiusMm", 2).put("objectFit", "cover").put("opacity", 0.5);
        assertThatCode(() -> validator.validate(doc.toString())).doesNotThrowAnyException();
        element.put("rotationDeg", 181);
        rejects(doc, "body[0].elements[0].rotationDeg", "INVALID_NUMBER");
        element.put("rotationDeg", 0).put("locked", "true");
        rejects(doc, "body[0].elements[0].locked", "UNSUPPORTED_VALUE");
        element.put("locked", true);
        ((ObjectNode) element.get("style")).put("objectFit", "none");
        rejects(doc, "body[0].elements[0].style.objectFit", "UNSUPPORTED_VALUE");
        ((ObjectNode) element.get("style")).put("objectFit", "cover").put("opacity", 1.5);
        rejects(doc, "body[0].elements[0].style.opacity", "INVALID_NUMBER");
    }

    @Test
    void persistsSafeStyleExtrasAndRejectsExecutableCss() throws Exception {
        var doc = document();
        ((ObjectNode) doc.at("/body/0/elements/0/style")).put("opacity", 0.4).put("letterSpacing", 0.2);
        var validated = validator.validate(doc.toString());
        assertThat(validated.canonicalJson()).contains("letterSpacing").contains("opacity");
        ((ObjectNode) doc.at("/body/0/elements/0/style")).put("backgroundImage", "url(https://example.invalid/a)");
        rejects(doc, "body[0].elements[0].style.backgroundImage", "UNKNOWN_PROPERTY");
    }

    @Test
    void acceptsTransparentAndPickerCellColors() throws Exception {
        var doc = document();
        ((ObjectNode) doc.at("/body/0/elements/0/style")).put("backgroundColor", "transparent");
        assertThatCode(() -> validator.validate(doc.toString())).doesNotThrowAnyException();
        ((ObjectNode) doc.at("/body/0/elements/0/style")).put("backgroundColor", "#11223344");
        assertThatCode(() -> validator.validate(doc.toString())).doesNotThrowAnyException();
        ((ObjectNode) doc.at("/body/0/elements/0/style")).put("backgroundColor", "rgba(17, 34, 51, 1)");
        assertThatCode(() -> validator.validate(doc.toString())).doesNotThrowAnyException();
        ((ObjectNode) doc.at("/body/0/elements/0/style")).put("backgroundColor", "red");
        rejects(doc, "body[0].elements[0].style.backgroundColor", "INVALID_COLOR");
    }

    @Test
    void enforcesGlobalElementLimitUtf8SizeAndInlineImageLimit() throws Exception {
        var doc = document();
        var body = doc.putArray("body");
        for (int group = 0; group < 2; group++) {
            var elements = body.addObject().put("id", "s" + group).put("kind", "FIXED").put("heightMm", 20).putArray("elements");
            for (int index = 0; index < 600; index++) {
                elements.addObject().put("id", "e" + group + "_" + index).put("type", "LINE").put("xMm", 0).put("yMm", 0).put("widthMm", 1).put("heightMm", 1);
            }
        }
        rejects(doc, "body", "TOO_MANY_ELEMENTS");
        String oversizedUtf8 = "\"" + "字".repeat(350000) + "\"";
        assertThatThrownBy(() -> validator.validate(oversizedUtf8)).isInstanceOf(PrintProtocolValidator.InvalidTemplateException.class);
        doc = document();
        ((ObjectNode) doc.at("/header/elements/0")).put("type", "IMAGE");
        ((ObjectNode) doc.at("/header/elements/0/binding")).put("value", "data:image/png;base64," + "A".repeat(700000));
        rejects(doc, "header.elements[0].binding", "INVALID_RESOURCE");
    }

    @Test
    void validatesNativeBlankTableCoverageAndPhysicalTracks() throws Exception {
        var doc = document();
        var element = (ObjectNode) doc.at("/body/0/elements/0");
        element.put("type", "STATIC_TABLE").put("widthMm", 60).put("heightMm", 10);
        element.remove("binding");
        var table = element.putObject("table");
        table.putArray("columns").addObject().put("id", "static_col_1").put("widthMm", 30);
        ((com.fasterxml.jackson.databind.node.ArrayNode) table.get("columns")).addObject().put("id", "static_col_2").put("widthMm", 30);
        table.putArray("rows").addObject().put("id", "static_row_1").put("heightMm", 10);
        var cells = table.putArray("cells");
        cells.addObject().put("id", "static_cell_1").put("row", 0).put("column", 0).put("rowSpan", 1).put("colSpan", 1).putObject("binding").put("source", "CONSTANT").put("value", "甲");
        cells.addObject().put("id", "static_cell_2").put("row", 0).put("column", 1).put("rowSpan", 1).put("colSpan", 1).putObject("binding").put("source", "FIELD").put("path", "main.name");
        assertThat(validator.validate(doc.toString()).document().body().get(0).elements().get(0).table().cells()).hasSize(2);
        ((ObjectNode) cells.get(0)).put("contentType", "IMAGE");
        ((ObjectNode) cells.get(0).get("binding")).put("value", "file_demo_1");
        assertThat(validator.validate(doc.toString()).document().body().get(0).elements().get(0).table().cells().get(0).contentType()).isEqualTo("IMAGE");
        ((ObjectNode) cells.get(1)).put("column", 0);
        rejects(doc, "body[0].elements[0].table", "INVALID_COVERAGE");
        ((ObjectNode) cells.get(1)).put("column", 1);
        element.put("widthMm", 61);
        rejects(doc, "body[0].elements[0].table", "TABLE_SIZE_MISMATCH");
    }

    @Test
    void acceptsDataTableAndHtmlElements() throws Exception {
        var doc = document();
        var element = (ObjectNode) doc.at("/body/0/elements/0");
        element.put("type", "DATA_TABLE").put("widthMm", 60).put("heightMm", 12);
        element.remove("binding");
        element.put("collectionPath", "children.items").put("repeatHeader", true).put("emptyText", "无明细");
        var columns = element.putArray("columns");
        columns.addObject().put("id", "dt_col_1").put("field", "children.items.name").put("title", "名称").put("widthMm", 30);
        columns.addObject().put("id", "dt_col_2").put("field", "children.items.qty").put("title", "数量").put("widthMm", 30);
        assertThat(validator.validate(doc.toString()).document().body().get(0).elements().get(0).type()).isEqualTo("DATA_TABLE");

        doc = document();
        element = (ObjectNode) doc.at("/body/0/elements/0");
        element.put("type", "HTML");
        var binding = element.putObject("binding");
        binding.put("source", "CONSTANT").put("value", "<b>说明</b>");
        assertThat(validator.validate(doc.toString()).document().body().get(0).elements().get(0).type()).isEqualTo("HTML");
    }

    @Test
    void invalidTemplateMessageIncludesFirstIssue() throws Exception {
        var doc = document();
        doc.put("schemaVersion", 2);
        assertThatThrownBy(() -> validator.validate(doc.toString()))
                .isInstanceOfSatisfying(PrintProtocolValidator.InvalidTemplateException.class, ex -> {
                    assertThat(ex.getMessage()).contains("打印模板校验失败：");
                    assertThat(ex.getMessage()).contains("schemaVersion");
                    assertThat(ex.getIssues()).isNotEmpty();
                });
    }

    @Test
    void validatesManualPageBreakPlacementAndPayload() throws Exception {
        var doc = document();
        var body = (com.fasterxml.jackson.databind.node.ArrayNode) doc.get("body");
        body.insert(1, mapper.createObjectNode().put("id", "manual_break").put("kind", "PAGE_BREAK"));
        assertThat(validator.validate(doc.toString()).document().body().get(1).kind()).isEqualTo("PAGE_BREAK");

        doc = document();
        body = (com.fasterxml.jackson.databind.node.ArrayNode) doc.get("body");
        body.insert(0, mapper.createObjectNode().put("id", "leading_break").put("kind", "PAGE_BREAK"));
        rejects(doc, "body[0]", "INVALID_PAGE_BREAK");

        doc = document();
        body = (com.fasterxml.jackson.databind.node.ArrayNode) doc.get("body");
        body.addObject().put("id", "trailing_break").put("kind", "PAGE_BREAK");
        rejects(doc, "body[3]", "INVALID_PAGE_BREAK");

        doc = document();
        body = (com.fasterxml.jackson.databind.node.ArrayNode) doc.get("body");
        body.insert(1, mapper.createObjectNode().put("id", "first_break").put("kind", "PAGE_BREAK"));
        body.insert(2, mapper.createObjectNode().put("id", "second_break").put("kind", "PAGE_BREAK"));
        rejects(doc, "body[2]", "INVALID_PAGE_BREAK");

        doc = document();
        body = (com.fasterxml.jackson.databind.node.ArrayNode) doc.get("body");
        body.insert(1, mapper.createObjectNode().put("id", "payload_break").put("kind", "PAGE_BREAK").put("gapAfterMm", 2));
        rejects(doc, "body[1].gapAfterMm", "UNKNOWN_PROPERTY");
    }

    @Test
    void acceptsWhitelistedExpressionAndRejectsEval() throws Exception {
        var doc = document();
        var binding = (ObjectNode) doc.at("/body/0/elements/0/binding");
        binding.remove("value");
        binding.remove("path");
        binding.put("source", "EXPRESSION");
        binding.put("expression", "MONEY(main.qty * main.price)");
        ((ObjectNode) doc.at("/body/0/elements/0")).putObject("format").put("type", "MONEY_UPPER");
        ((ObjectNode) doc.get("paper")).put("kind", "CONTINUOUS");
        doc.putObject("watermark").put("text", "内部资料").put("opacity", 0.08);
        doc.put("exportFileName", "{template}-{{main.code}}-{timestamp}");
        assertThat(validator.validate(doc.toString()).document().watermark().text()).isEqualTo("内部资料");
        assertThat(validator.validate(doc.toString()).document().exportFileName()).isEqualTo("{template}-{{main.code}}-{timestamp}");

        doc = document();
        binding = (ObjectNode) doc.at("/body/0/elements/0/binding");
        binding.remove("value");
        binding.remove("path");
        binding.put("source", "EXPRESSION");
        binding.put("expression", "eval(main.qty)");
        rejects(doc, "body[0].elements[0].binding.expression", "INVALID_EXPRESSION");
        doc = document();
        doc.put("exportFileName", "../secret");
        rejects(doc, "exportFileName", "INVALID_TEXT");
    }
}
