package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.Map;

/**
 * 扩展受限测试输入。
 */
@Data
public class BusinessExtensionTestDTO {

    private Map<String, Object> input;

    private String clientSandboxResult;
}
