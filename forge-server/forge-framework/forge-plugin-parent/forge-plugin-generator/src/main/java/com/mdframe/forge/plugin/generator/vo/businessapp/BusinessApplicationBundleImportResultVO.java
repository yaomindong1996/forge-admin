package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用调试包导入结果。
 */
@Data
public class BusinessApplicationBundleImportResultVO {

    private Long applicationId;

    private String applicationCode;

    private String applicationName;

    private List<BusinessApplicationBundleImportStepVO> steps = new ArrayList<>();

    private List<String> warnings = new ArrayList<>();
    private Boolean published;

    private Integer publishedVersion;
}
