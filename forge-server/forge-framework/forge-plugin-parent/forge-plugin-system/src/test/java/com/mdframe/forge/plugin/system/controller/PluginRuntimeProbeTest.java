package com.mdframe.forge.plugin.system.controller;

import com.mdframe.forge.plugin.system.config.PluginRuntimeProbeProperties;
import com.mdframe.forge.plugin.system.service.plugin.PluginRuntimeProbeService;
import com.mdframe.forge.starter.plugin.catalog.RuntimePluginCatalog;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PluginRuntimeProbeTest {
    @TempDir Path root;
    @Test void reads_actual_running_jar_not_configured_hash_and_refuses_ide_or_link() throws Exception {
        var file = root.resolve("admin.jar"); var bytes = new byte[]{'P', 'K', 3, 4, 5};
        Files.write(file, bytes);
        var config = new PluginRuntimeProbeProperties(); config.setJarPath(file.toString());
        var catalog = mock(RuntimePluginCatalog.class); when(catalog.getPlugins()).thenReturn(List.of());
        var service = new PluginRuntimeProbeService(config, catalog);
        String previous = System.getProperty("sun.java.command");
        try {
            System.setProperty("sun.java.command", "com.example.Application");
            assertThatThrownBy(() -> service.probe("test")).hasMessageContaining("JAR");
            System.setProperty("sun.java.command", file.toString());
            assertThat(service.probe("test").jarSha256()).isEqualTo(PackageDigests.sha256(bytes));
            var link = root.resolve("linked.jar"); Files.createSymbolicLink(link, file);
            config.setJarPath(link.toString()); System.setProperty("sun.java.command", link.toString());
            assertThatThrownBy(() -> service.probe("test")).hasMessageContaining("核验");
        } finally {
            if (previous == null) { System.clearProperty("sun.java.command"); }
            else { System.setProperty("sun.java.command", previous); }
        }
    }
}
