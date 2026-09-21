package com.mdframe.forge.plugin.print.dto;

import jakarta.validation.constraints.*;
import static com.mdframe.forge.plugin.print.protocol.PrintProtocolLimits.DOCUMENT_BYTES;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 固定输入协议；身份、租户、规范化来源与发布版本不接受客户端赋值。
 */
public record PrintTemplateCreateDTO(@NotNull @Positive Long applicationId, @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]{0,79}") String templateCode, @NotBlank @Size(max = 100) String templateName, @NotNull PrintSourceType sourceType, @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}") String pageId, @Size(max = 128) String formKey, @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,99}") String objectCode, @NotBlank @Size(max = DOCUMENT_BYTES) String schemaJson) {

    @JsonIgnore
    @AssertTrue(message = "低代码来源须提供 pageId，代码来源须提供 formKey，二者不能混用")
    public boolean isSourceValid() {
        // 由 @NotNull 报告。
        if (sourceType == null) {
            return true;
        }
        return switch(sourceType) {
            case LOWCODE ->
                pageId != null && formKey == null;
            case CODE ->
                pageId == null && formKey != null && !formKey.isBlank();
        };
    }
}
