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
        return object.path("objectId").asText().equals(ref.path("objectId").asText())
                && object.path("objectCode").asText().equals(ref.path("objectCode").asText())
                && object.path("configKey").asText().equals(ref.path("configKey").asText())
                && !object.path("configKey").asText().isBlank();
    }

    private void collectRefs(JsonNode node, List<JsonNode> refs) {
        if (node.isObject() && node.path("objectRef").isObject()) {
            refs.add(node.get("objectRef"));
        }
        if (node.isContainerNode()) {
            node.forEach(child -> collectRefs(child, refs));
        }
    }

    private RuntimeException invalid(PrintSourceRequest source) {
        return PrintFailure.field(source.pageId() + ":" + source.objectCode(), "打印页面与业务对象的绑定已变更");
    }
}
