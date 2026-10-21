package com.mdframe.forge.plugin.print.dto;

import jakarta.validation.constraints.*;
import static com.mdframe.forge.plugin.print.protocol.PrintProtocolLimits.DOCUMENT_BYTES;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 固定输入协议；身份、租户、规范化来源与发布版本不接受客户端赋值。
 */
public record PrintTemplateCreateDTO(
        @Positive Long businessSourceId,
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]{0,79}") String sourceCode,
        @Positive Long applicationId,
        @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]{0,79}") String templateCode,
        @NotBlank @Size(max = 100) String templateName,
        @NotNull PrintSourceType sourceType,
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}") String pageId,
        @Size(max = 128) String formKey,
        @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,99}") String objectCode,
        @NotBlank @Size(max = DOCUMENT_BYTES) String schemaJson,
        /** 市场/已安装模板导入草稿时允许字段尚未对齐当前来源目录；发布时仍会严格校验。 */
        Boolean allowUnresolvedFields) {

    public PrintTemplateCreateDTO(Long applicationId,
                                  String templateCode,
                                  String templateName,
                                  PrintSourceType sourceType,
                                  String pageId,
                                  String formKey,
                                  String objectCode,
                                  String schemaJson) {
        this(null, null, applicationId, templateCode, templateName, sourceType, pageId, formKey,
                objectCode, schemaJson, null);
    }

    public PrintTemplateCreateDTO(Long businessSourceId,
                                  String sourceCode,
                                  Long applicationId,
                                  String templateCode,
                                  String templateName,
                                  PrintSourceType sourceType,
                                  String pageId,
                                  String formKey,
                                  String objectCode,
                                  String schemaJson) {
        this(businessSourceId, sourceCode, applicationId, templateCode, templateName, sourceType, pageId, formKey,
                objectCode, schemaJson, null);
    }

    @JsonIgnore
    @AssertTrue(message = "打印来源身份无效")
    public boolean isSourceValid() {
        if (sourceType == null) {
            return true;
        }
        return new PrintSourceRequest(
                businessSourceId, sourceCode, applicationId, sourceType, pageId, formKey, objectCode)
                .isSourceValid();
    }

    public boolean allowsUnresolvedFields() {
        return Boolean.TRUE.equals(allowUnresolvedFields);
    }
}
