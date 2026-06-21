package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 低代码模型唯一性约束协议。
 */
@Data
public class LowcodeUniqueConstraintSchema {

    private String name;

    private List<String> fields = new ArrayList<>();

    /** TENANT-当前租户内唯一，后续预留 GLOBAL 等范围。 */
    private String scope;

    /** 归一化策略，例如 trim。 */
    private List<String> normalize = new ArrayList<>();

    private Boolean ignoreBlank;

    private String message;
}
