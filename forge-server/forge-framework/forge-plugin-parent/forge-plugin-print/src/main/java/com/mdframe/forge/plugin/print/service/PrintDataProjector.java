package com.mdframe.forge.plugin.print.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.print.spi.PrintData;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * 先按字段白名单投影，再序列化；从不把 Provider 的完整记录返回给浏览器。
 */
@Component
@RequiredArgsConstructor
public class PrintDataProjector {

    private final ObjectMapper mapper;

    private final PrintDocumentAccess documents;

    public record Projection(PrintData data, Set<String> fileIds) {
    }

    public Projection project(PrintData data, PrintDocumentAccess.Requirements requirements, PrintFieldCatalogVO catalog) {
        if (data == null) {
            throw PrintFailure.denied();
        }
        Map<String, Object> input = new HashMap<>();
        input.put("main", data.main());
        input.put("children", data.children());
        input.put("flow", data.flow());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("main", new LinkedHashMap<String, Object>());
        result.put("children", new LinkedHashMap<String, Object>());
        result.put("flow", new LinkedHashMap<String, Object>());
        var types = documents.catalog(catalog);
        Set<String> files = new LinkedHashSet<>(requirements.staticFileIds());
        for (String collection : requirements.collections()) {
            Object raw = get(input, collection);
            List<?> rows = raw == null ? List.of() : raw instanceof List<?> list ? list : null;
            if (rows == null || rows.size() > 500) {
                throw limit();
            }
            List<Map<String, Object>> output = new ArrayList<>();
            for (Object item : rows) {
                if (!(item instanceof Map<?, ?> row)) {
                    throw limit();
                }
                Map<String, Object> selected = new LinkedHashMap<>();
                for (String field : requirements.fields()) {
                    if (field.startsWith(collection + ".")) {
                        String relative = field.substring(collection.length() + 1);
                        Object value = scalar(get(row, relative), types.get(field), files, requirements.aliases());
                        put(selected, relative, value);
                    }
                }
                output.add(selected);
            }
            put(result, collection, output);
        }
        for (String field : requirements.fields()) {
            if (requirements.collections().stream().noneMatch(c -> field.startsWith(c + "."))) {
                put(result, field, scalar(get(input, field), types.get(field), files, requirements.aliases()));
            }
        }
        var projected = new PrintData(map(result.get("main")), map(result.get("children")), map(result.get("flow")));
        try (var output = new LimitedOutput()) {
            mapper.writeValue(output, projected);
        } catch (java.io.IOException ex) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("打印数据投影超过序列化范围");
            throw limit();
        }
        return new Projection(projected, Set.copyOf(files));
    }

    private static class LimitedOutput extends java.io.OutputStream {

        private long size;

        private void accept(int length) throws java.io.IOException {
            size += length;
            if (size > 4 * 1024 * 1024) {
                throw new java.io.IOException("print payload limit");
            }
        }

        @Override
        public void write(int value) throws java.io.IOException {
            accept(1);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws java.io.IOException {
            accept(length);
        }
    }

    private Object scalar(Object value, String type, Set<String> files, Map<String, String> aliases) {
        if (value == null) {
            return null;
        }
        // 数据集/JDBC 常返回时间等非 JSON 标量；先收敛成打印允许的 String/Number/Boolean
        Object normalized = normalizeScalar(value);
        if (normalized instanceof String text) {
            if (text.length() > 100000) {
                throw limit();
            }
            value = text;
        } else if (normalized instanceof Number number) {
            if (number.toString().length() > 100 || !Double.isFinite(number.doubleValue())) {
                throw limit();
            }
            value = number;
        } else if (normalized instanceof Boolean bool) {
            value = bool;
        } else {
            throw limit();
        }
        if ("IMAGE".equals(type)) {
            if (!(value instanceof String text)) {
                throw limit();
            }
            documents.image(aliases.getOrDefault(text, text), files);
        }
        return value;
    }

    private Object normalizeScalar(Object value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Character character) {
            return String.valueOf(character);
        }
        if (value instanceof Enum<?> enumerated) {
            return enumerated.name();
        }
        if (value instanceof java.time.temporal.TemporalAccessor
                || value instanceof java.util.Date
                || value instanceof Calendar) {
            return String.valueOf(value);
        }
        return value;
    }

    private Object get(Map<?, ?> root, String path) {
        Object value = root;
        for (String key : path.split("\\.")) {
            value = value instanceof Map<?, ?> nested ? nested.get(key) : null;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private void put(Map<String, Object> root, String path, Object value) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < keys.length - 1; i++) {
            Object next = current.computeIfAbsent(keys[i], ignored -> new LinkedHashMap<String, Object>());
            if (!(next instanceof Map<?, ?>)) {
                throw PrintFailure.denied();
            }
            current = map(next);
        }
        if (current.containsKey(keys[keys.length - 1])) {
            throw PrintFailure.denied();
        }
        current.put(keys[keys.length - 1], value);
    }

    private RuntimeException limit() {
        return PrintFailure.of(400, "PRINT_DATA_LIMIT", "打印数据类型或大小超过允许范围");
    }
}
