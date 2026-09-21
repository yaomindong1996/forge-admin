package com.mdframe.forge.plugin.print.dto;

import jakarta.validation.constraints.*;
import static com.mdframe.forge.plugin.print.protocol.PrintProtocolLimits.DOCUMENT_BYTES;

/**
 * 固定输入协议；身份、租户、规范化来源与发布版本不接受客户端赋值。
 */
public record PrintTemplateUpdateDTO(@NotNull @Min(1) @Max(9223372036854775806L) Long expectedRevision, @NotBlank @Size(max = 100) String templateName, @NotBlank @Size(max = DOCUMENT_BYTES) String schemaJson) {
}
