package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用协调发布参数。对象和入口空选择表示全部；扩展空选择时自动包含已测试、已启用或已停用扩展，
 * 未测试草稿保留但不阻断当前应用发布。
 */
@Data
public class BusinessApplicationPublishDTO {

    private List<Long> selectedObjectIds = new ArrayList<>();

    private List<Long> selectedEntryIds = new ArrayList<>();

    private List<Long> selectedExtensionIds = new ArrayList<>();

    private Boolean includeAutomation = true;

    private Boolean forceWarnings = false;

    private String remark;
}
