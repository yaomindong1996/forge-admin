package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 归档遗留锁只核查关闭结果，不赋予租约或重新执行任务。 */
@Data
public class PluginDeliveryRecoveryDTO {
    @NotNull @Pattern(regexp = "[a-f0-9-]{36}")
    private String nonce;
}
