package com.mdframe.forge.plugin.print.dto;

import jakarta.validation.constraints.*;

/**
 * 固定输入协议；身份、租户、规范化来源与发布版本不接受客户端赋值。
 */
public record PrintTemplateStatusDTO(@NotNull @Min(1) @Max(9223372036854775806L) Long expectedRevision, @NotNull @Min(0) @Max(1) Integer status) {
}
