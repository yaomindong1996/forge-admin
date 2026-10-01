package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用调试包导入步骤。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessApplicationBundleImportStepVO {

    private String key;

    private String label;

    /** PENDING / RUNNING / SUCCESS / FAILED / SKIPPED */
    private String status;

    private String message;
}
