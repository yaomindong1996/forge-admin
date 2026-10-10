package com.mdframe.forge.starter.plugin.catalog;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.mdframe.forge.starter.plugin.descriptor.PluginEdition;
import com.mdframe.forge.starter.plugin.version.SemanticVersion;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** 只扫描打包进当前服务的资源，不能用源码配置冒充运行时。 */
final class BuiltinModuleLoader {

    static final String PATTERN = "classpath*:META-INF/forge-module.json";
    private static final int MAX_BYTES = 16 * 1024;
    private static final ObjectReader READER = reader();

    private BuiltinModuleLoader() {
    }

    static List<RuntimePlugin> load(ResourcePatternResolver resolver) {
        try {
            List<RuntimePlugin> modules = new ArrayList<>();
            for (Resource resource : resolver.getResources(PATTERN)) {
                modules.add(read(resource));
            }
            return modules;
        } catch (IOException exception) {
            throw new IllegalStateException("扫描内置模块声明失败", exception);
        }
    }

    private static RuntimePlugin read(Resource resource) {
        try (InputStream input = resource.getInputStream()) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                throw new IllegalArgumentException("内置模块声明超过 16 KiB");
            }
            Module module = READER.readValue(bytes);
            validate(module);
            return new RuntimePlugin(module.id(), module.name(), module.version(), PluginOrigin.BUILTIN,
                    PluginEdition.COMMUNITY, null, module.module(), false, List.of());
        } catch (IOException exception) {
            throw new IllegalStateException("内置模块声明无法解析：" + resource.getDescription(), exception);
        }
    }

    private static void validate(Module module) {
        if (module == null || module.id() == null || !module.id().matches("[a-z][a-z0-9-]{0,63}")) {
            throw new IllegalArgumentException("内置模块 ID 不合法");
        }
        if (module.name() == null || module.name().isBlank() || module.name().length() > 100) {
            throw new IllegalArgumentException("内置模块名称不合法：" + module.id());
        }
        if (module.module() == null || !module.module().matches("[a-z][a-z0-9-]{0,127}")) {
            throw new IllegalArgumentException("内置模块 Maven 标识不合法：" + module.id());
        }
        SemanticVersion.parse(module.version());
    }

    private static ObjectReader reader() {
        JsonMapper mapper = JsonMapper.builder()
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        mapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
        return mapper.readerFor(Module.class);
    }

    private record Module(String id, String name, String version, String module) {
    }
}
