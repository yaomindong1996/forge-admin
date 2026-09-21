package com.mdframe.forge.plugin.print.protocol;

import static com.mdframe.forge.plugin.print.protocol.PrintProtocolLimits.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;

/**
 * 与前端协议一致的绑定、样式和格式白名单。
 */
final class PrintValueRules {

    private static final Set<String> FORBIDDEN = Set.of("__proto__", "prototype", "constructor");

    private static final Set<String> FORBIDDEN_STYLE = Set.of(
            "backgroundImage", "background", "src", "href", "content", "filter",
            "html", "innerHTML", "cssText", "expression", "clipPath", "mask", "cursor"
    );

    private final PrintProtocolRules r;

    PrintValueRules(PrintProtocolRules rules) {
        this.r = rules;
    }

    static boolean field(JsonNode value) {
        if (value == null || !value.isTextual() || value.textValue().length() > 300 || !value.textValue().matches("[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) {
            return false;
        }
        return java.util.Arrays.stream(value.textValue().split("\\.")).noneMatch(FORBIDDEN::contains);
    }

    static boolean fileId(JsonNode value) {
        return value != null && value.isTextual() && value.textValue().matches("[A-Za-z0-9_-]{1,128}");
    }

    void binding(JsonNode b, String path, boolean image, boolean fixedText) {
        if (!r.object(b, path, "source", "path", "value", "expression")) {
            return;
        }
        r.choice(b.get("source"), path + ".source", "CONSTANT", "FIELD", "SYSTEM", "EXPRESSION");
        String source = b.path("source").asText();
        if (source.equals("EXPRESSION")) {
            if (b.has("path") || b.has("value")) {
                r.issue(path, "CONFLICTING_BINDING", "表达式不能同时声明字段路径或固定值");
            }
            if (image) {
                r.issue(path, "INVALID_EXPRESSION", "图片不能使用表达式绑定");
            }
            PrintExpressionRules.check(b.get("expression"), path + ".expression", r);
            return;
        }
        if (b.has("expression")) {
            r.issue(path, "CONFLICTING_BINDING", "非表达式绑定不能声明 expression");
        }
        if (source.equals("CONSTANT")) {
            if (b.has("path")) {
                r.issue(path, "CONFLICTING_BINDING", "固定值不能同时声明字段路径");
            }
            JsonNode value = b.get("value");
            if (value == null || !(value.isTextual() || value.isNull() || value.isBoolean() || value.isNumber()) || (value.isNumber() && !Double.isFinite(value.doubleValue()))) {
                r.issue(path, "INVALID_CONSTANT", "固定值必须是文本、有限数字、布尔值或 null");
            }
            if (value != null && value.isTextual() && value.textValue().length() > TEXT_LENGTH) {
                r.issue(path, "TEXT_TOO_LONG", "文本超过限制");
            }
            if (image && truthy(value) && !safeImage(value)) {
                r.issue(path, "INVALID_RESOURCE", "图片须引用受控文件或受限内联图片");
            }
        } else {
            if (b.has("value")) {
                r.issue(path, "CONFLICTING_BINDING", "字段绑定不能同时声明固定值");
            }
            String field = b.path("path").asText();
            if (!field(b.get("path")) || (source.equals("SYSTEM") && !Set.of("system.generatedAt", "system.pageNumber", "system.totalPages").contains(field)) || (source.equals("FIELD") && field.startsWith("system."))) {
                r.issue(path + ".path", "INVALID_FIELD_PATH", "字段路径无效");
            }
            if (!fixedText && source.equals("SYSTEM") && Set.of("system.pageNumber", "system.totalPages").contains(field)) {
                r.issue(path, "INVALID_PAGE_NUMBER_BINDING", "页码只能绑定固定文本或页码元素");
            }
        }
    }

    private static boolean truthy(JsonNode value) {
        return value != null && !value.isNull() && !(value.isBoolean() && !value.booleanValue()) && !(value.isNumber() && value.doubleValue() == 0) && !(value.isTextual() && value.textValue().isEmpty());
    }

    static boolean isPrintColor(JsonNode value) {
        if (value == null || !value.isTextual()) {
            return false;
        }
        String raw = value.textValue().trim();
        if (raw.equalsIgnoreCase("transparent") || raw.equalsIgnoreCase("none")) {
            return true;
        }
        if (raw.matches("(?i)#(?:[0-9a-f]{3}|[0-9a-f]{4}|[0-9a-f]{6}|[0-9a-f]{8})")) {
            return true;
        }
        return raw.matches("(?i)rgba?\\(\\s*\\d+(?:\\.\\d+)?(?:[\\s,/]+\\d+(?:\\.\\d+)?){2}.*\\)");
    }

    private static boolean safeImage(JsonNode value) {
        if (fileId(value)) {
            return true;
        }
        return value.isTextual() && value.textValue().length() <= INLINE_IMAGE_BYTES * 4 / 3 && value.textValue().matches("(?i)data:image/(?:png|jpeg|webp);base64,[a-z0-9+/]+=*");
    }

    void style(JsonNode s, String path) {
        if (s == null || s.isMissingNode()) {
            return;
        }
        if (!s.isObject()) {
            r.issue(path, "INVALID_OBJECT", "必须是 JSON 对象");
            return;
        }
        s.fieldNames().forEachRemaining(key -> {
            String location = path + "." + key;
            if (FORBIDDEN.contains(key) || FORBIDDEN_STYLE.contains(key) || !key.matches("[A-Za-z][A-Za-z0-9]{0,40}")) {
                r.issue(location, "UNKNOWN_PROPERTY", "不支持此属性");
                return;
            }
            JsonNode item = s.get(key);
            if (item != null && (item.isObject() || item.isArray())) {
                r.issue(location, "INVALID_VALUE", "样式值必须是基础类型");
                return;
            }
            if (item != null && item.isTextual() && item.textValue().matches("(?i).*(url\\s*\\(|expression\\s*\\(|javascript:|<script).*")) {
                r.issue(location, "INVALID_VALUE", "样式值不安全");
            }
        });
        for (String key : List.of("color", "backgroundColor", "borderColor")) {
            if (s.has(key) && !isPrintColor(s.get(key))) {
                r.issue(path + "." + key, "INVALID_COLOR", "颜色须使用十六进制格式");
            }
        }
        if (s.has("fontFamily") && (!s.get("fontFamily").isTextual() || !s.get("fontFamily").textValue().matches("[\\p{L}\\p{N} ,_-]{1,100}"))) {
            r.issue(path + ".fontFamily", "INVALID_FONT", "字体名称无效");
        }
        if (s.has("fontSizePt")) {
            r.number(s.get("fontSizePt"), path + ".fontSizePt", 6, 144);
        }
        if (s.has("lineHeight")) {
            r.number(s.get("lineHeight"), path + ".lineHeight", 1, 4);
        }
        if (s.has("paddingMm")) {
            r.number(s.get("paddingMm"), path + ".paddingMm", 0, 20);
        }
        if (s.has("borderWidthMm")) {
            r.number(s.get("borderWidthMm"), path + ".borderWidthMm", 0, 3);
        }
        if (s.has("borderRadiusMm")) {
            r.number(s.get("borderRadiusMm"), path + ".borderRadiusMm", 0, 100);
        }
        if (s.has("fontWeight") && (!s.get("fontWeight").isNumber() || !Set.of(400.0, 700.0).contains(s.get("fontWeight").doubleValue()))) {
            r.issue(path + ".fontWeight", "UNSUPPORTED_VALUE", "字重只支持 400 或 700");
        }
        if (s.has("fontStyle")) {
            r.choice(s.get("fontStyle"), path + ".fontStyle", "normal", "italic");
        }
        if (s.has("textAlign")) {
            r.choice(s.get("textAlign"), path + ".textAlign", "left", "center", "right", "justify");
        }
        if (s.has("verticalAlign")) {
            r.choice(s.get("verticalAlign"), path + ".verticalAlign", "top", "middle", "bottom");
        }
        if (s.has("textDecoration")) {
            r.choice(s.get("textDecoration"), path + ".textDecoration", "none", "underline", "line-through", "overline");
        }
        if (s.has("borderStyle")) {
            r.choice(s.get("borderStyle"), path + ".borderStyle", "solid", "dashed", "dotted");
        }
        if (s.has("objectFit")) {
            r.choice(s.get("objectFit"), path + ".objectFit", "contain", "cover", "fill", "scale-down");
        }
        if (s.has("textFit")) {
            r.choice(s.get("textFit"), path + ".textFit", "CLIP", "SHRINK", "AUTO_HEIGHT");
        }
        if (s.has("shrinkMinFontSizePt")) {
            r.number(s.get("shrinkMinFontSizePt"), path + ".shrinkMinFontSizePt", 6, 144);
        }
        if (s.has("opacity")) {
            r.number(s.get("opacity"), path + ".opacity", 0, 1);
        }
    }

    void cellStyles(JsonNode value, String path) {
        if (value == null || !value.isObject()) {
            r.issue(path, "INVALID_OBJECT", "单元格样式映射无效");
            return;
        }
        value.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            if (!key.matches("^(header|data|footer|subtotal):\\d+:[\\w-]+$")) {
                r.issue(path + "." + key, "INVALID_CELL_STYLE_KEY", "单元格样式键无效");
                return;
            }
            style(entry.getValue(), path + "." + key);
        });
    }

    void format(JsonNode f, String path) {
        if (f == null || !r.object(f, path, "type", "scale", "emptyText", "trueText", "falseText", "datePattern")) {
            return;
        }
        r.choice(f.get("type"), path + ".type", "TEXT", "MONEY", "MONEY_UPPER", "NUMBER", "DATE", "BOOLEAN");
        if (f.has("scale")) {
            r.integer(f.get("scale"), path + ".scale", 0, 6);
        }
        for (String key : List.of("emptyText", "trueText", "falseText")) {
            if (f.has(key)) {
                r.text(f.get(key), path + "." + key, 100);
            }
        }
        if (f.has("datePattern")) {
            r.choice(f.get("datePattern"), path + ".datePattern", "YYYY-MM-DD", "YYYY-MM-DD HH:mm", "YYYY-MM-DD HH:mm:ss");
        }
    }
}
