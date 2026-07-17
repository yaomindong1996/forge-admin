package com.mdframe.forge.plugin.generator.dto.businessapp;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 编码规则生成请求。主要用于调试和内部集成验证。
 */
@Data
public class CodeRuleGenerateDTO {

    private String ruleCode;

    /** 新协议业务字段；只能用于 VARIABLE。 */
    @Size(max = 256, message = "业务上下文字段不能超过256个")
    private Map<String, Object> fields;

    /** 旧协议别名，兼容既有低代码调用。 */
    @Size(max = 256, message = "业务上下文字段不能超过256个")
    private Map<String, Object> context;
}
