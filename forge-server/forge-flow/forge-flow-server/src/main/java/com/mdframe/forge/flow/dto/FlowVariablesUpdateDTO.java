package com.mdframe.forge.flow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流程变量更新请求。变量键由流程模型动态定义，因此仅 variables 字段允许使用 Map。
 */
@Data
public class FlowVariablesUpdateDTO {

    @NotNull
    private Map<String, Object> variables = new LinkedHashMap<>();
}
