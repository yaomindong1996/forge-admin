package com.mdframe.forge.plugin.generator.manager.coderule;

import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSegmentDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyCodeRuleParserTest {

    @Test
    void shouldParseLegacyTemplateAndDegradeFineGrainedReset() {
        LegacyCodeRuleParser parser = new LegacyCodeRuleParser();

        List<CodeRuleSegmentDTO> segments = parser.parse(
                "PO${yyyyMMdd}${field:warehouseCode}-${tenantId}${seq:5}",
                "SECOND",
                4
        );

        assertEquals(List.of("FIXED", "DATE", "VARIABLE", "FIXED", "SYS_VAR", "SEQ"),
                segments.stream().map(CodeRuleSegmentDTO::getSegmentType).toList());
        assertEquals("CUSTOM", segments.get(2).getVariableSource());
        assertEquals(5, segments.get(5).getSegmentLength());
        assertEquals("HOUR", segments.get(5).getResetPolicy());

        List<CodeRuleSegmentDTO> automatic = parser.parse("DOC${yyyyMMdd}${seq:4}", "AUTO", 4);
        assertEquals("DAY", automatic.get(2).getResetPolicy());
    }

    @Test
    void shouldMaterializeKnownTemplatesAndRetainUnknownTokenAsVariable() {
        LegacyCodeRuleParser parser = new LegacyCodeRuleParser();
        List<String> templates = List.of(
                "WL${yyyyMMddHHmmss}${seq:3}",
                "DOC${yyyyMMdd}${seq:4}",
                "ORD${yyyyMMdd}${seq:5}",
                "HT${yyyyMM}${seq:5}",
                "C${seq:6}",
                "${orgCode}${yyyyMMdd}${seq:4}",
                "SUP${seq:5}",
                "WH${seq:4}",
                "CG${yyyyMMdd}${seq:4}",
                "CK${yyyyMMdd}${seq:4}",
                "DB${yyyyMMdd}${seq:4}"
        );
        for (String template : templates) {
            List<CodeRuleSegmentDTO> segments = parser.parse(template, "AUTO", 4);
            assertEquals(1, segments.stream().filter(segment -> "SEQ".equals(segment.getSegmentType())).count());
        }

        List<CodeRuleSegmentDTO> custom = parser.parse("X-${customToken}", "NONE", 4);
        assertEquals("VARIABLE", custom.get(1).getSegmentType());
        assertEquals("CUSTOM", custom.get(1).getVariableSource());
        assertEquals("customToken", custom.get(1).getSegmentValue());
    }
}
