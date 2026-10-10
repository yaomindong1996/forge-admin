package com.mdframe.forge.starter.plugin.descriptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginRegistryTest {

    @TempDir
    Path temp;

    @Test
    void should_allow_no_plugins() throws IOException {
        PluginRegistry registry = registry();
        assertThat(registry.getPlugins()).isEmpty();
        assertThat(registry.findById("missing")).isEmpty();
    }

    @Test
    void should_load_directory_and_jar_resources_in_id_order() throws IOException {
        Path classes = temp.resolve("classes");
        Files.createDirectories(classes.resolve("META-INF"));
        Files.writeString(classes.resolve("META-INF/forge-plugin.json"), descriptor("zeta", ">=1.2.0"));
        Path archive = temp.resolve("alpha.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(archive))) {
            jar.putNextEntry(new JarEntry("META-INF/forge-plugin.json"));
            jar.write(descriptor("alpha", ">=1.2.0").getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        URL[] urls = {classes.toUri().toURL(), archive.toUri().toURL()};
        try (URLClassLoader loader = new URLClassLoader(urls, getClass().getClassLoader())) {
            PluginRegistry registry = new PluginRegistry(new PathMatchingResourcePatternResolver(loader));
            assertThat(registry.getPlugins()).extracting(PluginDescriptor::id).containsExactly("alpha", "zeta");
            assertThat(registry.findById("zeta")).isPresent();
            assertThatThrownBy(() -> registry.getPlugins().clear()).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    void should_reject_duplicate_id_even_when_descriptors_equal() {
        String hello = descriptor("hello", ">=1.2.0 <2.0.0");
        assertThatIllegalArgumentException().isThrownBy(() -> registry(hello, hello))
                .withMessageContaining("id 重复").withMessageContaining("hello")
                .withMessageContaining(">=1.2.0 <2.0.0").withMessageContaining("当前核心 1.2.0");
    }

    @Test
    void should_reject_incompatible_or_invalid_descriptor() {
        assertThatIllegalArgumentException().isThrownBy(() -> registry(descriptor("hello", ">=2.0.0")))
                .withMessageContaining("hello").withMessageContaining(">=2.0.0")
                .withMessageContaining("当前核心 1.2.0");
        assertThatIllegalArgumentException().isThrownBy(() -> registry(descriptor("../hello", ">=1.2.0")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "[]", "{}{}", "{\"id\":\"hello\",\"id\":\"other\"}",
            "{\"id\":\"hello\",\"unexpected\":true}", "{\"edition\":\"unknown\"}"})
    void should_reject_malformed_ambiguous_or_unknown_json(String json) {
        assertThatThrownBy(() -> registry(json)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法读取/解析").hasMessageContaining("当前核心 1.2.0");
    }

    @Test
    void should_reject_null_descriptor_and_large_file() {
        assertThatIllegalArgumentException().isThrownBy(() -> registry("null")).withMessageContaining("描述不能为空");
        assertThatIllegalArgumentException().isThrownBy(() -> registry(" ".repeat(64 * 1024 + 1)))
                .withMessageContaining("超过 64 KiB");
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "42", "1.5"})
    void should_not_coerce_scalar_to_metadata_string(String scalar) {
        String hello = descriptor("hello", ">=1.2.0");
        assertThatThrownBy(() -> registry(hello.replace("\"id\":\"hello\"", "\"id\":" + scalar)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> registry(hello.replace("\"name\":\"示例插件\"", "\"name\":" + scalar)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> registry(hello.replace("[\"community.hello\"]", "[" + scalar + "]")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_fail_when_scanning_or_reading_resource_fails() throws IOException {
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        when(resolver.getResources(PluginRegistry.DESCRIPTOR_PATTERN)).thenThrow(new IOException("fixture scan error"));
        assertThatThrownBy(() -> new PluginRegistry(resolver)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("扫描插件描述失败");
        Resource resource = mock(Resource.class);
        when(resource.getDescription()).thenReturn("fixture unreadable");
        when(resource.getInputStream()).thenThrow(new IOException("fixture read error"));
        ResourcePatternResolver readableScan = mock(ResourcePatternResolver.class);
        when(readableScan.getResources(PluginRegistry.DESCRIPTOR_PATTERN)).thenReturn(new Resource[]{resource});
        assertThatThrownBy(() -> new PluginRegistry(readableScan)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fixture unreadable");
    }

    private PluginRegistry registry(String... descriptors) throws IOException {
        Resource[] resources = new Resource[descriptors.length];
        for (int index = 0; index < descriptors.length; index++) {
            resources[index] = new ByteArrayResource(descriptors[index].getBytes(StandardCharsets.UTF_8),
                    "fixture " + index);
        }
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        when(resolver.getResources(PluginRegistry.DESCRIPTOR_PATTERN)).thenReturn(resources);
        return new PluginRegistry(resolver);
    }

    private String descriptor(String id, String range) {
        return """
                {"id":"%s","name":"示例插件","version":"1.0.0","edition":"community",
                 "requiresCore":"%s","features":["community.hello"],"server":{"module":"forge-plugin-hello"}}
                """.formatted(id, range);
    }
}
