package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 应用工作台分区摘要。
 */
@Data
public class BusinessApplicationWorkspaceSectionVO {

    private String sectionKey;

    private String sectionName;

    private Long assetCount;

    private Long problemCount;

    private String status;

    private Boolean lazy;
}
