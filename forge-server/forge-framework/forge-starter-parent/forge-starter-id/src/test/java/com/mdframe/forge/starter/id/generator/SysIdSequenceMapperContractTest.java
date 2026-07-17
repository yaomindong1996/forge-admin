package com.mdframe.forge.starter.id.generator;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysIdSequenceMapperContractTest {

    @Test
    void legacyWatermarkQueryShouldUseEscapedIndexFriendlyPrefix() throws IOException {
        String xml = Files.readString(resolveMapper());
        int start = xml.indexOf("<select id=\"selectLegacyMaxId\"");
        int end = xml.indexOf("</select>", start);

        assertTrue(start >= 0);
        assertTrue(end > start);
        String query = xml.substring(start, end);
        assertTrue(query.contains("biz_key LIKE #{escapedLegacyKeyPattern}"));
        assertTrue(query.contains("ESCAPE '!'"));
        assertFalse(query.contains("LEFT(biz_key"));
    }

    private Path resolveMapper() {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            Path candidate = current.resolve("src/main/resources/mapper/SysIdSequenceMapper.xml");
            if (Files.exists(candidate)) {
                return candidate;
            }
            candidate = current.resolve(
                    "forge-server/forge-framework/forge-starter-parent/forge-starter-id"
                            + "/src/main/resources/mapper/SysIdSequenceMapper.xml");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Path.of("src/main/resources/mapper/SysIdSequenceMapper.xml");
    }
}
