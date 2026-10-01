package com.mdframe.forge.plugin.print.spi;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.entity.PrintBusinessSource;
import com.mdframe.forge.plugin.print.entity.PrintTemplate;
import com.mdframe.forge.plugin.print.dto.PrintTemplateCreateDTO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 稳定来源身份；展示名称、客户端 sourceKey 和 provider 名不参与授权。
 */
public record PrintSourceRequest(
        @Positive Long businessSourceId,
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]{0,79}") String sourceCode,
        @Positive Long applicationId,
        @NotNull PrintSourceType sourceType,
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}") String pageId,
        @Size(max = 128) String formKey,
        @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,99}") String objectCode) {

    public PrintSourceRequest {
        sourceCode = blankToNull(sourceCode);
        pageId = blankToNull(pageId);
        formKey = blankToNull(formKey);
    }

    public PrintSourceRequest(Long applicationId,
                              PrintSourceType sourceType,
                              String pageId,
                              String formKey,
                              String objectCode) {
        this(null, null, applicationId, sourceType, pageId, formKey, objectCode);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public boolean sameAs(PrintSourceRequest other) {
        return other != null
                && Objects.equals(applicationId, other.applicationId())
                && Objects.equals(businessSourceId, other.businessSourceId())
                && Objects.equals(key(), other.key());
    }

    @JsonIgnore
    @AssertTrue(message = "来源身份无效")
    public boolean isSourceValid() {
        if (sourceType == null) {
            return false;
        }
        if (sourceType.isStandalone()) {
            return businessSourceId != null && sourceCode != null && applicationId == null
                    && pageId == null && formKey == null;
        }
        if (businessSourceId != null || sourceCode != null || applicationId == null) {
            return false;
        }
        return sourceType == PrintSourceType.LOWCODE
                ? pageId != null && formKey == null
                : sourceType == PrintSourceType.CODE && pageId == null && formKey != null;
    }

    public String key() {
        // 旧应用来源必须保持历史摘要完全稳定，否则已发布绑定会整体失效。
        String raw = sourceType.isStandalone()
                ? sourceType + "\n" + businessSourceId + "\n" + sourceCode + "\n" + objectCode
                : sourceType + "\n" + pageId + "\n"
                + (formKey == null ? "" : formKey) + "\n" + objectCode;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return sourceType + ":" + HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            org.slf4j.LoggerFactory.getLogger(PrintSourceRequest.class).error("打印来源摘要算法不可用");
            throw new IllegalStateException("JVM 缺少 SHA-256", ex);
        }
    }

    public static PrintSourceRequest from(PrintTemplate row) {
        return new PrintSourceRequest(
                row.getBusinessSourceId(), row.getSourceCode(), row.getApplicationId(),
                PrintSourceType.valueOf(row.getSourceType()), row.getPageId(), row.getFormKey(),
                row.getObjectCode());
    }

    public static PrintSourceRequest from(PrintTemplateCreateDTO dto) {
        return new PrintSourceRequest(
                dto.businessSourceId(), dto.sourceCode(), dto.applicationId(), dto.sourceType(),
                dto.pageId(), dto.formKey(), dto.objectCode());
    }

    public static PrintSourceRequest from(PrintBusinessSource row) {
        return new PrintSourceRequest(
                row.getId(), row.getSourceCode(), null, PrintSourceType.valueOf(row.getSourceType()),
                null, null, row.getObjectCode());
    }
}
