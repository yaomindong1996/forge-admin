package com.mdframe.forge.plugin.print.spi;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.entity.PrintTemplate;
import com.mdframe.forge.plugin.print.dto.PrintTemplateCreateDTO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 稳定来源身份；展示名称、客户端 sourceKey 和 provider 名不参与授权。
 */
public record PrintSourceRequest(@NotNull @Positive Long applicationId, @NotNull PrintSourceType sourceType, @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}") String pageId, @Size(max = 128) String formKey, @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,99}") String objectCode) {

    @JsonIgnore
    @AssertTrue(message = "来源身份无效")
    public boolean isSourceValid() {
        return sourceType != null && (sourceType == PrintSourceType.LOWCODE ? pageId != null && formKey == null : pageId == null && formKey != null && !formKey.isBlank());
    }

    public String key() {
        String raw = sourceType + "\n" + pageId + "\n" + (formKey == null ? "" : formKey) + "\n" + objectCode;
        try {
            return sourceType + ":" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            org.slf4j.LoggerFactory.getLogger(PrintSourceRequest.class).error("打印来源摘要算法不可用");
            throw new IllegalStateException("JVM 缺少 SHA-256", ex);
        }
    }

    public static PrintSourceRequest from(PrintTemplate row) {
        return new PrintSourceRequest(row.getApplicationId(), PrintSourceType.valueOf(row.getSourceType()), row.getPageId(), row.getFormKey(), row.getObjectCode());
    }

    public static PrintSourceRequest from(PrintTemplateCreateDTO dto) {
        return new PrintSourceRequest(dto.applicationId(), dto.sourceType(), dto.pageId(), dto.formKey(), dto.objectCode());
    }
}
