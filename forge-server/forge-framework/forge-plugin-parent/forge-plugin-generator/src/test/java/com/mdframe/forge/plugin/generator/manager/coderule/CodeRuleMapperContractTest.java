package com.mdframe.forge.plugin.generator.manager.coderule;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
