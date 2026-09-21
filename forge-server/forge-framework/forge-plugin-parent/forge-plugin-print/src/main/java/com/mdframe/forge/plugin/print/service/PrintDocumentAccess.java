package com.mdframe.forge.plugin.print.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator.ValidatedDocument;
import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * 模板只能使用来源目录允许的字段和资源；目录由业务适配器提供。
 */
@Component
@RequiredArgsConstructor
public class PrintDocumentAccess {

    private final ObjectMapper mapper;

    private static final Set<String> TYPES = Set.of("TEXT", "NUMBER", "MONEY", "DATE", "BOOLEAN", "IMAGE", "COLLECTION");

    public record Requirements(Set<String> fields, Set<String> collections, Set<String> staticFileIds, Map<String, String> aliases) {

        public Requirements {
            fields = Set.copyOf(fields);
            collections = Set.copyOf(collections);
            staticFileIds = Set.copyOf(staticFileIds);
            aliases = Map.copyOf(aliases);
        }
    }

    public Requirements design(AuthorizedPrintSource source, PrintDataProvider provider, ValidatedDocument document) {
        var result = requirements(document, provider.catalog(source));
        provider.validateDesignResources(source, result.staticFileIds());
        return result;
    }

    public Map<String, String> catalog(PrintFieldCatalogVO catalog) {
        if (catalog == null || catalog.fields().size() > 2000) {
            throw PrintFailure.denied();
        }
        Map<String, String> types = new LinkedHashMap<>();
        for (var field : catalog.fields()) {
            if (field == null || !path(field.path()) || field.type() == null || !TYPES.contains(field.type()) || types.putIfAbsent(field.path(), field.type()) != null || field.label() == null || field.label().length() > 300) {
                throw PrintFailure.denied();
            }
        }
        for (var entry : types.entrySet()) {
            if (entry.getValue().equals("COLLECTION") && types.keySet().stream().anyMatch(p -> !p.equals(entry.getKey()) && entry.getKey().startsWith(p + "."))) {
                throw PrintFailure.denied();
            }
        }
        return Collections.unmodifiableMap(types);
    }

    private boolean path(String path) {
        if (path == null || path.length() > 300 || !path.matches("(main|children|flow)\\.[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) {
            return false;
        }
        return Arrays.stream(path.split("\\.")).noneMatch(p -> Set.of("__proto__", "constructor", "prototype").contains(p));
    }

    public Requirements requirements(ValidatedDocument document, PrintFieldCatalogVO catalog) {
        var types = catalog(catalog);
        var fields = new LinkedHashSet<String>();
        var collections = new LinkedHashSet<String>();
        var files = new LinkedHashSet<String>();
        var imageFields = new LinkedHashSet<String>();
        collect(mapper.valueToTree(document.document()), fields, collections, files, imageFields);
        for (String field : imageFields) {
            if (!"IMAGE".equals(types.get(field))) {
                throw PrintFailure.field(field, "图片元素必须绑定已授权的图片字段：" + field);
            }
        }
        for (String field : fields) {
            if (!types.containsKey(field) || "COLLECTION".equals(types.get(field))) {
                throw PrintFailure.field(field, "模板引用了未授权或已变更的字段：" + field);
            }
        }
        for (String collection : collections) {
            if (!"COLLECTION".equals(types.get(collection))) {
                throw PrintFailure.field(collection, "模板引用了未授权或已变更的明细集合：" + collection);
            }
        }
        Map<String, String> aliases = new HashMap<>();
        if (document.document().resources() != null) {
            for (var resource : document.document().resources()) {
                aliases.put(resource.id(), resource.fileId());
            }
        }
        Set<String> authorizedFiles = new LinkedHashSet<>(aliases.values());
        for (String file : files) {
            authorizedFiles.add(aliases.getOrDefault(file, file));
        }
        for (String field : fields) {
            for (var entry : types.entrySet()) {
                if ("COLLECTION".equals(entry.getValue()) && field.startsWith(entry.getKey() + ".") && !collections.contains(entry.getKey())) {
                    throw PrintFailure.field(field, "集合字段只能在对应明细表内使用：" + field);
                }
            }
        }
        return new Requirements(fields, collections, authorizedFiles, aliases);
    }

    private void collect(JsonNode node, Set<String> fields, Set<String> collections, Set<String> files, Set<String> imageFields) {
        if (node.isObject()) {
            var binding = node.path("binding");
            if ("FIELD".equals(binding.path("source").asText())) {
                fields.add(binding.path("path").asText());
                if ("IMAGE".equals(node.path("type").asText())) {
                    imageFields.add(binding.path("path").asText());
                }
            }
            if ("IMAGE".equals(node.path("type").asText()) && "CONSTANT".equals(binding.path("source").asText())) {
                image(binding.path("value").asText(), files);
            }
            if ("TABLE".equals(node.path("kind").asText())) {
                String collection = node.path("collectionPath").asText();
                collections.add(collection);
                for (var column : node.path("columns")) {
                    fields.add(collection + "." + column.path("field").asText());
                }
            }
        }
        if (node.isContainerNode()) {
            for (var child : node) {
                collect(child, fields, collections, files, imageFields);
            }
        }
    }

    public void image(String value, Set<String> files) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (value.matches("[A-Za-z0-9_-]{1,128}")) {
            files.add(value);
            return;
        }
        if (value.length() <= 400000 && value.matches("data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/]+={0,2}")) {
            return;
        }
        throw PrintFailure.of(400, "PRINT_RESOURCE_INVALID", "图片资源必须为授权文件标识或受限内嵌图片");
    }
}
