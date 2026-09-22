package com.mdframe.forge.plugin.generator.service.printing;

import com.fasterxml.jackson.databind.JsonNode;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 只检查当前页面实际引用的对象，不接受应用里任意其它对象。权限由调用方先核验。 */
@Component
public class LowcodePrintSourceResolver {
    private static final List<String> OBJECT_REF_FIELDS = List.of("objectRef", "businessObjectRef", "runtimeObjectRef");
    public JsonNode object(JsonNode snapshot, PrintSourceRequest source) {
        return object(snapshot, source, true);
    }

    public JsonNode object(JsonNode snapshot, PrintSourceRequest source, boolean allowLegacy) {
        if (source.sourceType() != PrintSourceType.LOWCODE) {
            throw PrintFailure.denied();
        }
        List<JsonNode> objects = new ArrayList<>();
        snapshot.path("objects").forEach(item -> {
            if (source.objectCode().equals(item.path("objectCode").asText())) {
                objects.add(item);
            }
        });
        if (objects.size() != 1) {
            throw invalid(source);
        }
        JsonNode object = objects.get(0);
        JsonNode options = snapshot.path("application").path("options");
        JsonNode builder = options.path("inAppBuilder");
        JsonNode page = builder.path("pages").path(source.pageId());
        List<JsonNode> refs = new ArrayList<>();
        boolean found = false;
        for (JsonNode node : builder.path("nodes")) {
            if (source.pageId().equals(node.path("id").asText()) && "page".equals(node.path("type").asText())) {
                found = true;
                collectRefs(node, refs);
            }
        }
        if (found) {
            collectRefs(page, refs);
            if (refs.stream().anyMatch(ref -> matches(ref, object))) {
                return object;
            }
        }
        // 与门户的存量单对象页面兼容；不能为已有页面树伪造额外入口。
        String token = source.objectCode().trim().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_").replaceAll("^_+|_+$", "");
        if (allowLegacy && builder.path("nodes").isEmpty() && builder.path("pages").isEmpty()
                && !builder.path("legacyObjectPageMigrated").asBoolean(false)
                && source.objectCode().equals(options.path("primaryObjectCode").asText())
                && source.pageId().equals("page_" + (token.isEmpty() ? "legacy_object" : token))) {
            return object;
        }
        throw invalid(source);
    }

    private boolean matches(JsonNode ref, JsonNode object) {
        if (ref.has("valid") && (!ref.path("valid").isBoolean() || !ref.path("valid").booleanValue())) {
            return false;
        }
        String objectConfigKey = object.path("configKey").asText("");
        if (objectConfigKey.isBlank()) {
            return false;
        }
        String objectObjectId = textFirst(object, "objectId", "id");
        String objectObjectCode = object.path("objectCode").asText("");
        String refObjectId = textFirst(ref, "objectId", "id");
        String refObjectCode = ref.path("objectCode").asText("");
        if (!refObjectId.isBlank() && !objectObjectId.isBlank()) {
            if (!objectObjectId.equals(refObjectId)) {
                return false;
            }
        } else if (!refObjectCode.isBlank() && !objectObjectCode.isBlank()) {
            if (!objectObjectCode.equals(refObjectCode)) {
                return false;
            }
        } else {
            return false;
        }
        if (!refObjectCode.isBlank() && !objectObjectCode.equals(refObjectCode)) {
            return false;
        }
        String refConfigKey = ref.path("configKey").asText("");
        if (refConfigKey.isBlank()) {
            return true;
        }
        return objectConfigKey.equals(refConfigKey);
    }

    private void collectRefs(JsonNode node, List<JsonNode> refs) {
        if (node.isObject()) {
            appendObjectRefs(node, refs);
            JsonNode props = node.path("props");
            if (props.isObject()) {
                appendObjectRefs(props, refs);
            }
        }
        if (node.isContainerNode()) {
            node.forEach(child -> collectRefs(child, refs));
        }
    }

    private void appendObjectRefs(JsonNode node, List<JsonNode> refs) {
        for (String field : OBJECT_REF_FIELDS) {
            if (node.path(field).isObject()) {
                refs.add(node.get(field));
            }
        }
    }

    private String textFirst(JsonNode node, String primary, String fallback) {
        String primaryValue = node.path(primary).asText("");
        if (!primaryValue.isBlank()) {
            return primaryValue;
        }
        return node.path(fallback).asText("");
    }

    private RuntimeException invalid(PrintSourceRequest source) {
        return PrintFailure.field(source.pageId() + ":" + source.objectCode(), "打印页面与业务对象的绑定已变更");
    }
}
