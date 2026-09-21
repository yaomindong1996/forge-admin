package com.mdframe.forge.plugin.generator.service.printing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import java.util.List;
import java.util.Map;

final class PrintApplicationTestData {
    static final PrintSourceRequest SOURCE = new PrintSourceRequest(2L, PrintSourceType.LOWCODE,
            "page_purchase", null, "purchase");
    static final String SCHEMA = """
            {"protocol":"forge-print","schemaVersion":1,
             "paper":{"widthMm":210,"heightMm":297,"orientation":"PORTRAIT",
                      "marginMm":{"top":10,"right":10,"bottom":10,"left":10}},
             "header":{"heightMm":0,"repeat":true,"elements":[]},
             "body":[{"id":"title","kind":"TEXT","binding":{"source":"CONSTANT","value":"合成单据"}}],
             "footer":{"heightMm":0,"repeat":true,"elements":[]},"resources":[]}
            """;
    static final String HASH = new PrintProtocolValidator().validate(SCHEMA).schemaHash();
    static PrintApplicationSnapshotCodec.Binding binding(long templateId, boolean isDefault) {
        return new PrintApplicationSnapshotCodec.Binding(SOURCE, PrintScene.DETAIL, templateId,
                20L, HASH, isDefault, 0);
    }
    static String snapshot(PrintApplicationSnapshotCodec.Binding... bindings) {
        try {
            return new ObjectMapper().writeValueAsString(Map.of("printing",
                    Map.of("schemaVersion", 1, "bindings", List.of(bindings))));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
