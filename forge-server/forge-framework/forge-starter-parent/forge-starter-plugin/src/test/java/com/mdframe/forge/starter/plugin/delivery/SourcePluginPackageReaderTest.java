package com.mdframe.forge.starter.plugin.delivery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SourcePluginPackageReaderTest {
    private static final String META = """
            {"id":"demo","name":"测试插件","version":"1.0.0","edition":"community",
            "requiresCore":">=1.2.0 <2.0.0","features":["community.demo"],"ui":{"dir":"ui"}}
            """;

    @ParameterizedTest
    @ValueSource(ints = {ZipEntry.STORED, ZipEntry.DEFLATED})
    void accepts_valid_stored_and_deflate_with_bounded_public_summary(int method) throws Exception {
        byte[] zip = archive(files(), method);
        SourcePluginPackage result = SourcePluginPackageReader.read(zip, "1.2.0");
        assertThat(result.descriptor().id()).isEqualTo("demo");
        assertThat(result.sha256()).isEqualTo(PackageDigests.sha256(zip));
        assertThat(result.fileCount()).isEqualTo(2);
        assertThat(result.files()).allSatisfy(file -> assertThat(file.sha256()).hasSize(64));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../evil", "/evil", "a/../evil", "a\\evil", "C:evil", "a//b", "a./b",
            "nul/file", "a/.env.local", "a/application-dev.yml", "a/.git/config", "a/target/x",
            "a/.forge-plugin-owned.json"})
    void rejects_unsafe_or_private_paths(String path) throws Exception {
        var files = files();
        files.put(path, bytes("evil"));
        reject(archive(files, ZipEntry.STORED));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UI/index.vue", "ui/index.vue/child"})
    void rejects_case_and_file_directory_conflicts(String name) throws Exception {
        var files = files();
        files.put(name, bytes("other"));
        reject(archive(files, ZipEntry.STORED));
    }

    @Test
    void rejects_normalized_unicode_duplicate_paths() throws Exception {
        var files = files();
        files.put("ui/é.txt", bytes("a"));
        files.put("ui/e\u0301.txt", bytes("b"));
        reject(archive(files, ZipEntry.STORED));
    }

    @Test
    void rejects_symlink_mode_and_encryption_zip64_unknown_method() throws Exception {
        byte[] source = archive(files(), ZipEntry.STORED);
        int central = central(source);
        byte[] link = source.clone();
        buffer(link).putInt(central + 38, 0xa1ff << 16);
        reject(link);
        byte[] encrypted = source.clone();
        buffer(encrypted).putShort(central + 8, (short) 0x801);
        reject(encrypted);
        byte[] zip64 = source.clone();
        buffer(zip64).putShort(central + 6, (short) 45);
        reject(zip64);
        byte[] unknown = source.clone();
        buffer(unknown).putShort(central + 10, (short) 99);
        reject(unknown);
    }

    @Test
    void rejects_local_name_mismatch_crc_truncation_and_limits() throws Exception {
        byte[] source = archive(files(), ZipEntry.STORED);
        byte[] name = source.clone();
        name[30] = 'x';
        reject(name);
        byte[] crc = source.clone();
        buffer(crc).putInt(central(crc) + 16, 0);
        reject(crc);
        byte[] localCrc = source.clone();
        buffer(localCrc).putInt(14, 0);
        reject(localCrc);
        byte[] localSize = source.clone();
        buffer(localSize).putInt(22, 0);
        reject(localSize);
        byte[] bomb = source.clone();
        buffer(bomb).putInt(central(bomb) + 24, PluginZipReader.MAX_FILE + 1);
        reject(bomb);
        reject(java.util.Arrays.copyOf(source, source.length - 1));
        reject(new byte[PluginZipReader.MAX_ARCHIVE + 1]);
    }

    @Test
    void rejects_duplicate_json_keys_coercion_and_trailing_tokens() throws Exception {
        for (String value : new String[]{META.replace("\"id\":\"demo\"", "\"id\":false"),
                META.replace("\"id\":\"demo\"", "\"id\":\"demo\",\"id\":\"other\""), META + "{}"}) {
            var files = files();
            files.put("forge-plugin.json", bytes(value));
            reject(archive(files, ZipEntry.STORED));
        }
    }

    @Test
    void rejects_missing_metadata_ui_and_incompatible_core() throws Exception {
        reject(archive(Map.of("ui/index.vue", bytes("x")), ZipEntry.STORED));
        reject(archive(Map.of("forge-plugin.json", bytes(META)), ZipEntry.STORED));
        byte[] zip = archive(files(), ZipEntry.STORED);
        assertThatIllegalArgumentException().isThrownBy(() -> SourcePluginPackageReader.read(zip, "2.0.0"));
    }

    @Test
    void validates_matching_runtime_descriptor_single_module_pom_and_no_external_entities() throws Exception {
        var files = files();
        String metadata = META.replace("\"ui\":", "\"server\":{\"module\":\"forge-plugin-demo\"},\"ui\":");
        String base = "server/forge-plugin-demo/";
        String group = String.join(".", "com", "mdframe", "forge");
        String pom = "<project><parent><groupId>" + group + "</groupId><artifactId>forge-server</artifactId>"
                + "<version>${revision}</version></parent><artifactId>forge-plugin-demo</artifactId></project>";
        files.put("forge-plugin.json", bytes(metadata));
        files.put(base + "src/main/resources/META-INF/forge-plugin.json", bytes(metadata));
        files.put(base + "pom.xml", bytes(pom));
        assertThat(SourcePluginPackageReader.read(archive(files, ZipEntry.DEFLATED), "1.2.0").fileCount()).isEqualTo(4);
        files.put(base + "src/main/resources/META-INF/forge-plugin.json", bytes(META));
        reject(archive(files, ZipEntry.STORED));
        files.put(base + "src/main/resources/META-INF/forge-plugin.json", bytes(metadata));
        files.put(base + "pom.xml", bytes("<!DOCTYPE project [<!ENTITY x SYSTEM 'file:///not-read'>]>" + pom));
        reject(archive(files, ZipEntry.STORED));
        String modules = pom.replace("</project>", "<modules><module>x</module></modules></project>");
        files.put(base + "pom.xml", bytes(modules));
        reject(archive(files, ZipEntry.STORED));
        files.put(base + "pom.xml", bytes(pom.replace("</project>", "<x>".repeat(65) + "</x>".repeat(65)
                + "</project>")));
        reject(archive(files, ZipEntry.STORED));
    }

    @Test
    void caps_preview_without_hiding_total_file_count() throws Exception {
        var files = files();
        for (int index = 0; index < 210; index++) {
            files.put("ui/file-" + index, bytes("a"));
        }
        SourcePluginPackage value = SourcePluginPackageReader.read(archive(files, ZipEntry.STORED), "1.2.0");
        assertThat(value.fileCount()).isEqualTo(212);
        assertThat(value.files()).hasSize(200);
    }

    @Test
    void rejects_excess_entries_and_total_expansion_even_for_highly_compressed_archives() throws Exception {
        var many = files();
        for (int index = 0; index < 2000; index++) {
            many.put("ui/small-" + index, bytes("a"));
        }
        reject(archive(many, ZipEntry.DEFLATED));
        var bomb = files();
        for (int index = 0; index < 9; index++) {
            bomb.put("ui/large-" + index, new byte[PluginZipReader.MAX_FILE]);
        }
        byte[] compressed = archive(bomb, ZipEntry.DEFLATED);
        assertThat(compressed.length).isLessThan(PluginZipReader.MAX_ARCHIVE);
        reject(compressed);
    }

    private Map<String, byte[]> files() {
        return new LinkedHashMap<>(Map.of("forge-plugin.json", bytes(META), "ui/index.vue", bytes("<template/>")));
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private void reject(byte[] zip) {
        assertThatIllegalArgumentException().isThrownBy(() -> SourcePluginPackageReader.read(zip, "1.2.0"));
    }

    private ByteBuffer buffer(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    private int central(byte[] bytes) {
        return buffer(bytes).getInt(bytes.length - 6);
    }

    private byte[] archive(Map<String, byte[]> files, int method) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (var file : files.entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                entry.setMethod(method);
                CRC32 crc = new CRC32();
                crc.update(file.getValue());
                entry.setSize(file.getValue().length);
                entry.setCrc(crc.getValue());
                zip.putNextEntry(entry);
                zip.write(file.getValue());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }
}
