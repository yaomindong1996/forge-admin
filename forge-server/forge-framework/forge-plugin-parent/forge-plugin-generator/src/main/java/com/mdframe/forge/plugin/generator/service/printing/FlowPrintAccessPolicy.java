package com.mdframe.forge.plugin.generator.service.printing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.spi.AuthorizedPrintContext;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 待办、已办、我发起使用不同的身份门槛，并在服务端收敛节点字段/模板范围。 */
@Component
@RequiredArgsConstructor
public class FlowPrintAccessPolicy {

    private final ObjectMapper json;

    public void authorize(PrintActor actor, PrintScene scene, FlowPrintContextResolver.Context context) {
        String userId = String.valueOf(actor.userId());
        boolean allowed = switch (scene) {
            case FLOW_TODO -> context.taskId() != null
                    && context.taskStatus() != null
                    && (context.taskStatus() == 0 || context.taskStatus() == 1)
                    && (StringUtils.isBlank(context.assignee()) || userId.equals(context.assignee()));
            case FLOW_DONE -> context.taskId() != null
                    && context.taskStatus() != null
                    && context.taskStatus() != 0
                    && context.taskStatus() != 1
                    && (userId.equals(context.assignee()) || userId.equals(context.owner()));
            case FLOW_STARTED -> context.taskId() == null && userId.equals(context.startUserId());
            default -> false;
        };
        if (!allowed) {
            throw PrintFailure.denied();
        }
    }

    public List<AuthorizedPrintContext.VersionRef> templates(
            List<AuthorizedPrintContext.VersionRef> source,
            FlowPrintContextResolver.Context context) {
        if (!"RESTRICT".equalsIgnoreCase(StringUtils.trimToEmpty(context.printTemplatePolicy()))) {
            return source;
        }
        Set<Long> allowed = new HashSet<>();
        for (String value : StringUtils.defaultString(context.printTemplateIds()).split(",")) {
            String text = value.trim();
            if (text.matches("[1-9][0-9]{0,18}")) {
                allowed.add(Long.valueOf(text));
            }
        }
        if (allowed.isEmpty()) {
            return List.of();
        }
        return source.stream().filter(item -> allowed.contains(item.templateId())).toList();
    }

    public PrintFieldCatalogVO fields(PrintFieldCatalogVO catalog,
                                      FlowPrintContextResolver.Context context) {
        Hidden hidden = hiddenFields(context.formFieldPermissions());
        if (hidden == null) {
            return catalog;
        }
        List<PrintFieldCatalogVO.Field> result = new ArrayList<>();
        for (PrintFieldCatalogVO.Field field : catalog.fields()) {
            if (hidden.allows(field.path())) {
                result.add(field);
            }
        }
        return new PrintFieldCatalogVO(result);
    }

    private Hidden hiddenFields(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        try {
            JsonNode root = json.readTree(raw);
            JsonNode fields = root.isArray() ? root : root.path("fields");
            if (!fields.isArray()) {
                throw PrintFailure.denied();
            }
            Set<String> main = new HashSet<>();
            Map<String, Set<String>> children = new LinkedHashMap<>();
            for (JsonNode item : fields) {
                String scope = StringUtils.defaultIfBlank(item.path("scope").asText(), "main");
                boolean childScope = "child".equalsIgnoreCase(scope) || "array".equalsIgnoreCase(scope);
                String field = childScope
                        ? first(item, "childField", "itemField", "field")
                        : first(item, "field", "fieldCode", "code");
                boolean readable = !item.has("readable") || item.path("readable").asBoolean(true);
                if (StringUtils.isBlank(field) || readable) {
                    continue;
                }
                if (childScope) {
                    String key = first(item, "childKey", "arrayKey");
                    if (StringUtils.isNotBlank(key)) {
                        children.computeIfAbsent(key, ignored -> new HashSet<>()).add(field);
                    }
                } else {
                    main.add(field);
                }
            }
            Set<String> hiddenChildren = new HashSet<>();
            JsonNode childConfigs = root.path("children");
            if (childConfigs.isArray()) {
                for (JsonNode item : childConfigs) {
                    String key = first(item, "childKey", "arrayKey", "key");
                    if (StringUtils.isNotBlank(key) && item.has("readable")
                            && !item.path("readable").asBoolean(true)) {
                        hiddenChildren.add(key);
                    }
                }
            }
            return new Hidden(main, children, hiddenChildren);
        } catch (java.io.IOException | IllegalArgumentException error) {
            throw PrintFailure.denied();
        }
    }

    private record Hidden(Set<String> main,
                          Map<String, Set<String>> children,
                          Set<String> hiddenChildren) {
        private boolean allows(String path) {
            if (path == null || path.startsWith("flow.")) {
                return true;
            }
            if (path.startsWith("main.")) {
                return !main.contains(path.substring("main.".length()));
            }
            if (!path.startsWith("children.")) {
                return false;
            }
            String[] parts = path.split("\\.", 3);
            if (parts.length < 2 || hiddenChildren.contains(parts[1])) {
                return false;
            }
            return parts.length == 2 || !children.getOrDefault(parts[1], Set.of()).contains(parts[2]);
        }
    }

    private String first(JsonNode item, String... keys) {
        return Arrays.stream(keys)
                .map(item::path)
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .map(StringUtils::trimToNull)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(null);
    }
}
