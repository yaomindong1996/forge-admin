package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionVersion;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.ExtensionExecutionContext;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.ExtensionExecutionResult;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.ExtensionInputField;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.LowcodeExtensionHandler;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.LowcodeExtensionRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessExtensionValidationService")
class BusinessExtensionValidationServiceTest {

    @Test
    @DisplayName("client script rejects browser network storage and dynamic code APIs")
    void clientScriptRejectsDangerousApis() {
        var result = service(List.of()).validate(
                extension("CLIENT_JS", "BEFORE_SUBMIT"),
                version("fetch('https://example.com')", null, "{}"));

        assertFalse(result.isPassed());
        assertTrue(result.getSummary().contains("禁止 API"));
    }

    @Test
    @DisplayName("client script cannot bypass the effects protocol with postMessage")
    void clientScriptRejectsDirectPostMessage() {
        var result = service(List.of()).validate(
                extension("CLIENT_JS", "BEFORE_SUBMIT"),
                version("postMessage({ leaked: true })", null, "{}"));

        assertFalse(result.isPassed());
        assertTrue(result.getSummary().contains("postMessage"));
    }

    @Test
    @DisplayName("scoped css rejects global roots and requires processed application scope")
    void scopedCssRejectsGlobalRoots() {
        var result = service(List.of()).validate(
                extension("SCOPED_CSS", "PAGE_INIT"),
                version("body { display: none; }", "body{display:none}", "{}"));

        assertFalse(result.isPassed());
        assertTrue(result.getSummary().contains("全局") || result.getSummary().contains("作用域"));
    }

    @Test
    @DisplayName("scoped css only binds the page initialization hook")
    void scopedCssRejectsSubmitHook() {
        String scope = "[data-forge-app=\"crm\"][data-forge-page=\"form\"]";
        var result = service(List.of()).validate(
                extension("SCOPED_CSS", "BEFORE_SUBMIT"),
                version(".card { color: blue; }", scope + " .card{color:blue}",
                        "{\"scopeSelector\":\"" + scope.replace("\"", "\\\"") + "\"}"));

        assertFalse(result.isPassed());
        assertTrue(result.getSummary().contains("不支持钩子"));
    }

    @Test
    @DisplayName("scoped css rejects a forged processed payload with one unprefixed selector")
    void scopedCssRejectsForgedProcessedPayload() {
        String scope = "[data-forge-app=\"crm\"][data-forge-page=\"form\"]";
        var result = service(List.of()).validate(
                extension("SCOPED_CSS", "PAGE_INIT"),
                version(".card { color: blue; }", scope + " .card{color:blue}.escape{display:none}",
                        "{\"scopeSelector\":\"" + scope.replace("\"", "\\\"") + "\"}"));

        assertFalse(result.isPassed());
        assertTrue(result.getSummary().contains("未限制"));
    }

    @Test
    @DisplayName("scoped css accepts ordinary selectors nested in an allowed media rule")
    void scopedCssAcceptsSafeNestedRules() {
        String scope = "[data-forge-app=\"crm\"][data-forge-page=\"form\"]";
        var result = service(List.of()).validate(
                extension("SCOPED_CSS", "PAGE_INIT"),
                version("@media (min-width: 900px) { .card { color: blue; } }",
                        "@media (min-width:900px){" + scope + " .card{color:blue}}",
                        "{\"scopeSelector\":\"" + scope.replace("\"", "\\\"") + "\"}"));

        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("server binding rejects arbitrary class metadata even when JSON is syntactically valid")
    void serverBindingRejectsArbitraryClassMetadata() {
        var result = service(List.of()).validate(
                extension("SERVER_BINDING", "BEFORE_SUBMIT"),
                version("{}", null, "{\"handlerCode\":\"java.lang.Runtime\",\"className\":\"java.lang.Runtime\"}"));

        assertFalse(result.isPassed());
        assertTrue(result.getSummary().contains("未注册") || result.getSummary().contains("Class"));
    }

    @Test
    @DisplayName("registered handler passes only on an explicitly allowed hook")
    void registeredHandlerPassesAllowedHook() {
        var result = service(List.of(handler())).validate(
                extension("SERVER_BINDING", "BEFORE_SUBMIT"),
                version("{}", null, "{\"handlerCode\":\"customer_validation\"}"));

        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("registered handler rejects a hook outside its declared contract")
    void registeredHandlerRejectsUndeclaredHook() {
        var result = service(List.of(handler())).validate(
                extension("SERVER_BINDING", "AFTER_SUBMIT"),
                version("{}", null, "{\"handlerCode\":\"customer_validation\"}"));

        assertFalse(result.isPassed());
        assertTrue(result.getSummary().contains("处理器不允许"));
    }

    private BusinessExtensionValidationService service(List<LowcodeExtensionHandler> handlers) {
        return new BusinessExtensionValidationService(new ObjectMapper(), new LowcodeExtensionRegistry(handlers));
    }

    private AiBusinessExtension extension(String type, String hook) {
        AiBusinessExtension extension = new AiBusinessExtension();
        extension.setExtensionType(type);
        extension.setHookCode(hook);
        return extension;
    }

    private AiBusinessExtensionVersion version(String content, String processedContent, String configJson) {
        AiBusinessExtensionVersion version = new AiBusinessExtensionVersion();
        version.setContent(content);
        version.setProcessedContent(processedContent);
        version.setConfigJson(configJson);
        return version;
    }

    private LowcodeExtensionHandler handler() {
        return new LowcodeExtensionHandler() {
            @Override
            public String handlerCode() {
                return "customer_validation";
            }

            @Override
            public String handlerName() {
                return "客户校验";
            }

            @Override
            public Set<String> allowedHooks() {
                return Set.of("BEFORE_SUBMIT");
            }

            @Override
            public Map<String, ExtensionInputField> inputSchema() {
                return Map.of();
            }

            @Override
            public ExtensionExecutionResult execute(ExtensionExecutionContext context) {
                return ExtensionExecutionResult.success(Map.of());
            }
        };
    }
}
