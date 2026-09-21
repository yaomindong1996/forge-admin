package com.mdframe.forge.plugin.print.protocol;

import static com.mdframe.forge.plugin.print.protocol.PrintProtocolLimits.*;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * 验证与规范化是单次纯计算；不修改输入、不解析业务字段、不执行模板内容。
 */
@Component
public final class PrintProtocolValidator {

    public static final int MAX_DOCUMENT_BYTES = DOCUMENT_BYTES;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PrintProtocolValidator.class);

    private final ObjectMapper mapper = new ObjectMapper(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(JSON_DEPTH).maxStringLength(MAX_DOCUMENT_BYTES).maxNumberLength(1000).build()).build()).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);

    public ValidatedDocument validate(String json) {
        if (json == null || json.length() > MAX_DOCUMENT_BYTES || json.getBytes(StandardCharsets.UTF_8).length > MAX_DOCUMENT_BYTES) {
            throw invalid("INVALID_JSON", "模板必须是最多 1 MiB 的 JSON");
        }
        JsonNode tree;
        try {
            tree = mapper.readTree(json);
        } catch (java.io.IOException | IllegalArgumentException ex) {
            // 不将 parser 错误中的原始数据片段返回前端或写入日志。
            LOG.debug("拒绝无效打印 JSON，异常类型：{}", ex.getClass().getSimpleName());
            throw invalid("INVALID_JSON", "JSON 无效、存在重复属性或嵌套超过限制");
        }
        var rules = new PrintProtocolRules();
        rules.document(tree);
        if (!rules.issues.isEmpty()) {
            throw new InvalidTemplateException(rules.issues);
        }
        try {
            JsonNode canonical = canonicalize(tree);
            String text = mapper.writeValueAsString(canonical);
            var document = mapper.treeToValue(canonical, PrintTemplateDocument.class);
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
            return new ValidatedDocument(document, text, hash);
        } catch (java.io.IOException ex) {
            LOG.warn("打印协议模型转换失败，异常类型：{}", ex.getClass().getSimpleName());
            throw invalid("INVALID_TEMPLATE", "模板与协议模型不一致");
        } catch (NoSuchAlgorithmException ex) {
            LOG.error("JVM 缺少标准 SHA-256 算法", ex);
            throw new IllegalStateException("JVM 缺少 SHA-256", ex);
        }
    }

    private JsonNode canonicalize(JsonNode value) {
        if (value.isObject()) {
            var result = mapper.createObjectNode();
            List<String> keys = new ArrayList<>();
            value.fieldNames().forEachRemaining(keys::add);
            keys.sort(String::compareTo);
            keys.forEach(key -> result.set(key, canonicalize(value.get(key))));
            return result;
        }
        if (value.isArray()) {
            var result = mapper.createArrayNode();
            value.forEach(item -> result.add(canonicalize(item)));
            return result;
        }
        if (value.isNumber()) {
            return DecimalNode.valueOf(value.decimalValue().stripTrailingZeros());
        }
        return value;
    }

    private InvalidTemplateException invalid(String code, String message) {
        return new InvalidTemplateException(List.of(new Issue("", code, message)));
    }

    public record Issue(String path, String code, String message) {
    }

    public record ValidatedDocument(PrintTemplateDocument document, String canonicalJson, String schemaHash) {
    }

    public static final class InvalidTemplateException extends BusinessException {

        private final List<Issue> issues;

        public InvalidTemplateException(List<Issue> issues) {
            super(400, summarize(issues), List.copyOf(issues));
            this.issues = List.copyOf(issues);
        }

        public List<Issue> getIssues() {
            return issues;
        }

        private static String summarize(List<Issue> issues) {
            if (issues == null || issues.isEmpty()) {
                return "打印模板校验失败";
            }
            Issue first = issues.get(0);
            String detail = first.message() == null || first.message().isBlank()
                    ? first.code()
                    : first.message();
            if (first.path() != null && !first.path().isBlank()) {
                detail = detail + "（" + first.path() + "）";
            }
            if (issues.size() == 1) {
                return "打印模板校验失败：" + detail;
            }
            return "打印模板校验失败：" + detail + " 等 " + issues.size() + " 项";
        }
    }
}
