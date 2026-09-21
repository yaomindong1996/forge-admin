package com.mdframe.forge.plugin.generator.service.printing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 只解析版本固定引用；不读取设计态绑定，不把损坏快照降级为空。 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PrintApplicationSnapshotCodec {
    private static final int MAX_SNAPSHOT_CHARS = 64 * 1024 * 1024;
    private static final int MAX_BINDINGS = 1000;
    private static final int MAX_SOURCE_TEMPLATES = 100;
    private static final Set<String> BINDING_KEYS = Set.of("source", "scene", "templateId",
            "templateVersionId", "schemaHash", "isDefault", "sortOrder");
    private static final Set<String> SOURCE_KEYS = Set.of("applicationId", "sourceType", "pageId",
            "formKey", "objectCode");
    private final Validator validator;
    private final ObjectMapper json = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(128)
                    .maxStringLength(MAX_SNAPSHOT_CHARS).build()).build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public record Binding(PrintSourceRequest source, PrintScene scene, Long templateId,
                          Long templateVersionId, String schemaHash, boolean isDefault, int sortOrder) { }

    public List<Binding> read(String snapshotJson, Long applicationId) {
        if (snapshotJson == null || snapshotJson.isBlank() || snapshotJson.length() > MAX_SNAPSHOT_CHARS) {
            throw invalid();
        }
        try {
            JsonNode root = json.readTree(snapshotJson);
            if (root == null || !root.isObject()) {
                throw invalid();
            }
            if (!root.has("printing")) {
                return List.of();
            }
            JsonNode printing = root.get("printing");
            keys(printing, Set.of("schemaVersion", "bindings"));
            if (!printing.path("schemaVersion").isIntegralNumber()
                    || !printing.path("schemaVersion").canConvertToInt()
                    || printing.path("schemaVersion").intValue() != 1
                    || !printing.path("bindings").isArray()
                    || printing.path("bindings").size() > MAX_BINDINGS) {
                throw invalid();
            }
            List<Binding> result = new ArrayList<>();
            Set<String> unique = new HashSet<>();
            Set<String> defaults = new HashSet<>();
            Map<String, Integer> scopeSizes = new HashMap<>();
            for (JsonNode item : printing.get("bindings")) {
                Binding binding = binding(item, applicationId);
                String scope = binding.source().key() + ":" + binding.scene();
                if (!unique.add(scope + ":" + binding.templateId())
                        || (binding.isDefault() && !defaults.add(scope))
                        || scopeSizes.merge(scope, 1, Integer::sum) > MAX_SOURCE_TEMPLATES) {
                    throw invalid();
                }
                result.add(binding);
            }
            return List.copyOf(result);
        } catch (java.io.IOException | IllegalArgumentException ex) {
            log.debug("拒绝无效应用打印快照，异常类型：{}", ex.getClass().getSimpleName());
            throw invalid();
        }
    }

    private Binding binding(JsonNode item, Long applicationId) {
        keys(item, BINDING_KEYS);
        JsonNode sourceNode = item.path("source");
        keys(sourceNode, SOURCE_KEYS);
        PrintSourceRequest source = new PrintSourceRequest(id(sourceNode.path("applicationId")),
                PrintSourceType.valueOf(text(sourceNode.path("sourceType"))),
                optionalText(sourceNode.get("pageId")), optionalText(sourceNode.get("formKey")),
                text(sourceNode.path("objectCode")));
        if (!source.applicationId().equals(applicationId) || !validator.validate(source).isEmpty()
                || !item.path("isDefault").isBoolean() || !item.path("sortOrder").isIntegralNumber()
                || !item.path("sortOrder").canConvertToInt()) {
            throw invalid();
        }
        int sortOrder = item.get("sortOrder").intValue();
        String hash = text(item.path("schemaHash"));
        if (sortOrder < 0 || sortOrder > 10000 || !hash.matches("[a-f0-9]{64}")) {
            throw invalid();
        }
        return new Binding(source, PrintScene.valueOf(text(item.path("scene"))),
                id(item.path("templateId")), id(item.path("templateVersionId")), hash,
                item.get("isDefault").booleanValue(), sortOrder);
    }

    private void keys(JsonNode node, Set<String> keys) {
        if (node == null || !node.isObject()) {
            throw invalid();
        }
        node.fieldNames().forEachRemaining(key -> {
            if (!keys.contains(key)) {
                throw invalid();
            }
        });
    }

    private Long id(JsonNode value) {
        if (!(value.isTextual() || value.isIntegralNumber()) || !value.asText().matches("[1-9][0-9]{0,18}")) {
            throw invalid();
        }
        return Long.valueOf(value.asText());
    }

    private String text(JsonNode value) {
        if (!value.isTextual()) {
            throw invalid();
        }
        return value.textValue();
    }

    private String optionalText(JsonNode value) {
        return value == null || value.isNull() ? null : text(value);
    }

    private BusinessException invalid() {
        return PrintFailure.of(409, "PRINT_APPLICATION_SNAPSHOT_INVALID", "应用打印快照无效，请重新检查发布配置");
    }
}
