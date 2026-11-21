package com.mdframe.forge.plugin.ai.skill.pkg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillPackageArchiveTest {

    private final SkillPackageArchive archive = new SkillPackageArchive(new ObjectMapper());

    @Test
    void readShouldSynthesizeManifestFromFrontMatterAndUnwrapSingleRootFolder() throws IOException {
        String skillMd = """
                ---
                name: Order Query Guide
                description: Guide for querying sales orders
                version: 1.2.0
                ---
                # Order Query Guide
                Use this guide to answer order status questions.
                """;
        byte[] zip = zipOf(Map.of(
                "order-guide/SKILL.md", skillMd,
                "order-guide/reference.md", "## Status Codes\nPAID, SHIPPED"
        ));

        SkillPackageArchive.PackageContent content = archive.read(zip);

        assertEquals("order_query_guide", content.manifest().code());
        assertEquals("1.2.0", content.manifest().version());
        assertEquals("Order Query Guide", content.manifest().name());
        assertNotNull(content.packageHash());
        assertEquals(64, content.packageHash().length());
        assertTrue(content.files().stream().anyMatch(f -> "SKILL.md".equals(f.path())));
        assertTrue(content.files().stream().anyMatch(f -> "reference.md".equals(f.path())));
        assertTrue(content.files().stream().anyMatch(f -> "skill.json".equals(f.path())));
    }

    @Test
    void readShouldRejectZipWithoutSkillMdOrWithTraversalPath() throws IOException {
        byte[] noSkillMd = zipOf(Map.of("README.md", "hello"));
        assertThrows(BusinessException.class, () -> archive.read(noSkillMd));

        byte[] traversal = zipOf(Map.of(
                "SKILL.md", "# ok",
                "../escape.md", "bad"
        ));
        assertThrows(BusinessException.class, () -> archive.read(traversal));
    }

    private byte[] zipOf(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            for (var entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
