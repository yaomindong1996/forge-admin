package com.mdframe.forge.plugin.generator.dto.businessapp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BusinessFlowModelBindingsQueryDTO {
    @NotEmpty(message = "请选择流程模型")
    @Size(max = 100, message = "一次最多查询 100 个流程模型")
    private List<@NotBlank(message = "模型 Key 不能为空")
            @Size(max = 128, message = "模型 Key 不能超过 128 字符") String> modelKeys;
}
