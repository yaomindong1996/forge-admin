package com.mdframe.forge.plugin.generator.manager.coderule;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeRuleMapperContractTest {

    @Test
    void selectableRulesShouldIncludeGenericAndCurrentObjectBindingsOnly() throws IOException {
        String xml = Files.readString(resolveMapper());

        assertTrue(xml.contains("source_object_code IS NULL"));
        assertTrue(xml.contains("source_object_code = ''"));
        assertTrue(xml.contains("source_object_code = #{sourceObjectCode}"));
        assertTrue(xml.contains("OR source_object_code = #{sourceObjectCode}"));
        assertTrue(xml.contains("<otherwise>"));
    }

    @Test
    void segmentMapperShouldRoundTripVariableSource() throws IOException {
        String xml = Files.readString(resolveSegmentMapper());

        assertTrue(xml.contains("column=\"variable_source\" property=\"variableSource\""));
        assertTrue(xml.contains("segment_value, variable_source"));
        assertTrue(xml.contains("#{segment.variableSource}"));
    }

    @Test
    void ruleCodeHistoryCheckShouldIncludeLogicallyDeletedRules() throws IOException {
        String xml = Files.readString(resolveMapper());
        int start = xml.indexOf("<select id=\"countRuleCodeHistory\"");
        int end = xml.indexOf("</select>", start);

        assertTrue(start >= 0);
        assertTrue(end > start);
        String query = xml.substring(start, end);
        assertTrue(query.contains("rule_code = #{ruleCode}"));
        assertFalse(query.contains("del_flag"));
    }

    @Test
    void ruleMapperShouldRoundTripLegacyCompatibilityFlag() throws IOException {
        String xml = Files.readString(resolveMapper());

        assertTrue(xml.contains("column=\"legacy_compat_enabled\" property=\"legacyCompatEnabled\""));
        assertTrue(xml.contains("legacy_compat_enabled,"));
    }

    private Path resolveMapper() {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            Path candidate = current.resolve("src/main/resources/mapper/CodeRuleMapper.xml");
            if (Files.exists(candidate)) {
                return candidate;
            }
            candidate = current.resolve(
                    "forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator"
                            + "/src/main/resources/mapper/CodeRuleMapper.xml");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Path.of("src/main/resources/mapper/CodeRuleMapper.xml");
    }

    private Path resolveSegmentMapper() {
        Path mapper = resolveMapper();
        return mapper.resolveSibling("CodeRuleSegmentMapper.xml");
    }
}
