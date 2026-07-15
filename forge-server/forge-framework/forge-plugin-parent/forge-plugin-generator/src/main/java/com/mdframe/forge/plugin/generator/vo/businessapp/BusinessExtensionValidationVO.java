package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 扩展校验或测试摘要。
 */
@Data
public class BusinessExtensionValidationVO {

    private boolean passed;

    private String summary;

    private List<String> issues = new ArrayList<>();
}
