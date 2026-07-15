package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessExtensionHook;
import com.mdframe.forge.plugin.generator.constant.BusinessExtensionType;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionVersion;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.LowcodeExtensionHandler;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.LowcodeExtensionRegistry;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessExtensionValidationVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 扩展内容的服务端失败关闭校验。
 */
@Service
@RequiredArgsConstructor
public class BusinessExtensionValidationService {

    private static final Pattern FORBIDDEN_CLIENT_API = Pattern.compile(
            "(?i)(?:\\bwindow\\b|\\bdocument\\b|\\bglobalThis\\b|\\bself\\b|\\blocalStorage\\b|"
                    + "\\bsessionStorage\\b|\\bindexedDB\\b|\\bcaches\\b|\\bcookie\\b|\\bfetch\\s*\\(|"
                    + "\\bXMLHttpRequest\\b|\\bWebSocket\\b|\\bEventSource\\b|\\bimportScripts\\b|\\bpostMessage\\b|"
                    + "\\beval\\s*\\(|\\bnew\\s+Function\\b|\\.constructor\\b|__proto__|\\.prototype\\b|"
                    + "\\bsetTimeout\\b|\\bsetInterval\\b|\\bWorker\\b|\\bSharedWorker\\b)");
    private static final Pattern FORBIDDEN_CSS = Pattern.compile(
            "(?is)(@import\\b|url\\s*\\(|expression\\s*\\(|(?:^|[,{])\\s*(?:html|body|:root)\\b|"
                    + "forge[-_ ]?layout|layout[-_ ]?(?:sider|sidebar)|app[-_ ]?sidebar|#app\\b|"
                    + "position\\s*:\\s*fixed\\b|z-index\\s*:\\s*(?:[1-9][0-9]{2,}|[1-9][0-9]{3,})\\b)");
    private static final Pattern SECRET_LITERAL = Pattern.compile(
            "(?i)\\b(?:password|client[_-]?secret|api[_-]?key|access[_-]?token|authorization|cookie)\\b\\s*[:=]");
    private static final Pattern SCOPE_SELECTOR = Pattern.compile(
            "^\\[data-forge-app=\"[A-Za-z0-9_-]{1,128}\"\\]"
                    + "\\[data-forge-page=\"[A-Za-z0-9_-]{1,128}\"\\]$");
    private static final Set<String> CLIENT_API_NAMES = Set.of(
            "readField", "setField", "showMessage", "triggerAction"
    );

    private final ObjectMapper objectMapper;
    private final LowcodeExtensionRegistry extensionRegistry;

    public BusinessExtensionValidationVO validate(AiBusinessExtension extension,
                                                   AiBusinessExtensionVersion version) {
        BusinessExtensionValidationVO result = new BusinessExtensionValidationVO();
        if (extension == null || version == null) {
            result.getIssues().add("扩展或当前草稿版本不存在");
            return finish(result);
        }
        String type = StringUtils.defaultString(extension.getExtensionType()).toUpperCase(Locale.ROOT);
        if (!BusinessExtensionHook.allowedForType(type).contains(extension.getHookCode())) {
            result.getIssues().add("当前扩展类型不支持钩子: " + extension.getHookCode());
        }
        switch (type) {
            case BusinessExtensionType.VISUAL_RULE -> validateVisualRule(version, result);
            case BusinessExtensionType.CLIENT_JS -> validateClientJs(version, result);
            case BusinessExtensionType.SCOPED_CSS -> validateScopedCss(version, result);
            case BusinessExtensionType.SERVER_BINDING -> validateServerBinding(extension, version, result);
            default -> result.getIssues().add("不支持的扩展类型");
        }
        return finish(result);
    }

    private void validateVisualRule(AiBusinessExtensionVersion version, BusinessExtensionValidationVO result) {
        try {
            JsonNode root = objectMapper.readTree(version.getContent());
            if (root == null || !root.isObject()) {
                result.getIssues().add("可视化规则必须是结构化 JSON 对象");
                return;
            }
            if (!root.path("conditions").isArray() || !root.path("actions").isArray()) {
                result.getIssues().add("可视化规则必须包含条件和动作列表");
            }
            if (root.path("actions").isArray() && root.path("actions").isEmpty()) {
                result.getIssues().add("可视化规则至少需要一个动作");
            }
        } catch (Exception e) {
            result.getIssues().add("可视化规则 JSON 格式不正确");
        }
    }

    private void validateClientJs(AiBusinessExtensionVersion version, BusinessExtensionValidationVO result) {
        String content = StringUtils.defaultString(version.getContent());
        if (StringUtils.isBlank(content)) {
            result.getIssues().add("客户端脚本不能为空");
            return;
        }
        if (content.indexOf('`') >= 0 || content.matches("(?s).*\\\\(?:u[0-9a-fA-F]{4}|x[0-9a-fA-F]{2}).*")) {
            result.getIssues().add("客户端脚本不能使用模板字符串或转义标识符");
        }
        var matcher = FORBIDDEN_CLIENT_API.matcher(stripCommentsAndStrings(content));
        if (matcher.find()) {
            result.getIssues().add("客户端脚本包含禁止 API: " + matcher.group());
        }
        if (SECRET_LITERAL.matcher(content).find()) {
            result.getIssues().add("客户端脚本不能包含明文密钥或认证信息");
        }
        if (content.contains("import(") || content.contains("import ")) {
            result.getIssues().add("客户端脚本不能动态加载模块");
        }
        if (content.length() > BusinessExtensionSecurityPolicy.MAX_CONTENT_LENGTH) {
            result.getIssues().add("客户端脚本超过大小限制");
        }
    }

    private void validateScopedCss(AiBusinessExtensionVersion version, BusinessExtensionValidationVO result) {
        String content = StringUtils.defaultString(version.getContent());
        String processed = StringUtils.defaultString(version.getProcessedContent());
        var matcher = FORBIDDEN_CSS.matcher(stripCssComments(content));
        if (matcher.find()) {
            result.getIssues().add("样式包含全局或外部资源规则: " + matcher.group());
        }
        if (StringUtils.isBlank(processed)
                || !processed.contains("[data-forge-app=\"")
                || !processed.contains("[data-forge-page=\"")) {
            result.getIssues().add("样式尚未生成应用和页面作用域结果");
        }
        if (FORBIDDEN_CSS.matcher(stripCssComments(processed)).find()) {
            result.getIssues().add("作用域样式仍包含越界规则");
        }
        try {
            JsonNode config = objectMapper.readTree(version.getConfigJson());
            String scopeSelector = config == null ? null : config.path("scopeSelector").asText(null);
            if (StringUtils.isBlank(scopeSelector) || !SCOPE_SELECTOR.matcher(scopeSelector).matches()) {
                result.getIssues().add("样式作用域配置不正确");
            } else {
                validateScopedRules(stripCssComments(processed), scopeSelector, result);
            }
        } catch (Exception e) {
            result.getIssues().add("样式作用域配置格式不正确");
        }
    }

    private void validateServerBinding(AiBusinessExtension extension, AiBusinessExtensionVersion version,
                                       BusinessExtensionValidationVO result) {
        try {
            JsonNode config = objectMapper.readTree(version.getConfigJson());
            String handlerCode = config == null ? null : config.path("handlerCode").asText(null);
            if (StringUtils.isBlank(handlerCode)) {
                result.getIssues().add("服务端扩展必须选择平台注册处理器");
                return;
            }
            LowcodeExtensionHandler handler = extensionRegistry.find(handlerCode).orElse(null);
            if (handler == null) {
                result.getIssues().add("服务端扩展处理器未注册或不可用");
                return;
            }
            if (handler.allowedHooks() == null || !handler.allowedHooks().contains(extension.getHookCode())) {
                result.getIssues().add("处理器不允许绑定当前钩子");
            }
            if (config.has("beanName") || config.has("className") || config.has("classPath")
                    || config.has("url") || config.has("token") || config.has("secret")) {
                result.getIssues().add("服务端扩展不能保存 Bean/Class、URL 或明文密钥");
            }
        } catch (Exception e) {
            result.getIssues().add("服务端扩展配置格式不正确");
        }
    }

    private BusinessExtensionValidationVO finish(BusinessExtensionValidationVO result) {
        result.setPassed(result.getIssues().isEmpty());
        result.setSummary(result.isPassed() ? "校验通过" : String.join("；", result.getIssues()));
        return result;
    }

    private String stripCommentsAndStrings(String source) {
        StringBuilder result = new StringBuilder(source.length());
        boolean lineComment = false;
        boolean blockComment = false;
        char quote = 0;
        for (int index = 0; index < source.length(); index++) {
            char current = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : 0;
            if (lineComment) {
                if (current == '\n') {
                    lineComment = false;
                    result.append('\n');
                } else {
                    result.append(' ');
                }
                continue;
            }
            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    result.append("  ");
                    index++;
                } else {
                    result.append(' ');
                }
                continue;
            }
            if (quote != 0) {
                if (current == '\\') {
                    result.append("  ");
                    index++;
                } else if (current == quote) {
                    quote = 0;
                    result.append(' ');
                } else {
                    result.append(' ');
                }
                continue;
            }
            if (current == '/' && next == '/') {
                lineComment = true;
                result.append("  ");
                index++;
            } else if (current == '/' && next == '*') {
                blockComment = true;
                result.append("  ");
                index++;
            } else if (current == '\'' || current == '"' || current == '`') {
                quote = current;
                result.append(' ');
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }

    private String stripCssComments(String source) {
        return StringUtils.defaultString(source).replaceAll("(?s)/\\*.*?\\*/", " ");
    }

    private void validateScopedRules(String css, String scopeSelector, BusinessExtensionValidationVO result) {
        int index = 0;
        while (index < css.length()) {
            index = skipWhitespace(css, index);
            if (index >= css.length()) {
                return;
            }
            int openBrace = findNextBrace(css, index);
            if (openBrace < 0) {
                if (StringUtils.isNotBlank(css.substring(index))) {
                    result.getIssues().add("作用域样式存在未闭合规则");
                }
                return;
            }
            String header = css.substring(index, openBrace).trim();
            int closeBrace = findMatchingBrace(css, openBrace);
            if (closeBrace < 0) {
                result.getIssues().add("作用域样式存在未闭合大括号");
                return;
            }
            if (header.startsWith("@")) {
                String atRule = header.substring(1).split("[\\s(]", 2)[0].toLowerCase(Locale.ROOT);
                if (!Set.of("media", "supports", "container", "layer").contains(atRule)) {
                    result.getIssues().add("作用域样式包含不允许的规则: @" + atRule);
                } else {
                    validateScopedRules(css.substring(openBrace + 1, closeBrace), scopeSelector, result);
                }
            } else {
                for (String selector : splitSelectors(header)) {
                    String normalized = selector.trim();
                    if (!(normalized.equals(scopeSelector) || normalized.startsWith(scopeSelector + " "))) {
                        result.getIssues().add("处理后选择器未限制到应用页面作用域: " + normalized);
                    }
                }
            }
            index = closeBrace + 1;
        }
    }

    private int skipWhitespace(String value, int start) {
        int index = start;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private int findNextBrace(String value, int start) {
        char quote = 0;
        int parentheses = 0;
        int brackets = 0;
        for (int index = start; index < value.length(); index++) {
            char current = value.charAt(index);
            if (quote != 0) {
                if (current == '\\') {
                    index++;
                } else if (current == quote) {
                    quote = 0;
                }
                continue;
            }
            if (current == '\'' || current == '"') {
                quote = current;
            } else if (current == '(') {
                parentheses++;
            } else if (current == ')') {
                parentheses = Math.max(0, parentheses - 1);
            } else if (current == '[') {
                brackets++;
            } else if (current == ']') {
                brackets = Math.max(0, brackets - 1);
            } else if (current == '{' && parentheses == 0 && brackets == 0) {
                return index;
            }
        }
        return -1;
    }

    private int findMatchingBrace(String value, int openBrace) {
        int depth = 1;
        char quote = 0;
        for (int index = openBrace + 1; index < value.length(); index++) {
            char current = value.charAt(index);
            if (quote != 0) {
                if (current == '\\') {
                    index++;
                } else if (current == quote) {
                    quote = 0;
                }
                continue;
            }
            if (current == '\'' || current == '"') {
                quote = current;
            } else if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return index;
            }
        }
        return -1;
    }

    private List<String> splitSelectors(String selectorList) {
        List<String> selectors = new java.util.ArrayList<>();
        int start = 0;
        int parentheses = 0;
        int brackets = 0;
        for (int index = 0; index < selectorList.length(); index++) {
            char current = selectorList.charAt(index);
            if (current == '(') {
                parentheses++;
            } else if (current == ')') {
                parentheses = Math.max(0, parentheses - 1);
            } else if (current == '[') {
                brackets++;
            } else if (current == ']') {
                brackets = Math.max(0, brackets - 1);
            } else if (current == ',' && parentheses == 0 && brackets == 0) {
                selectors.add(selectorList.substring(start, index));
                start = index + 1;
            }
        }
        selectors.add(selectorList.substring(start));
        return selectors;
    }
}
