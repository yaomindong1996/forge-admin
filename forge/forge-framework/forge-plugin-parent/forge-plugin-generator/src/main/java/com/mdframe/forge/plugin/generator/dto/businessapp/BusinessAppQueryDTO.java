package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 应用入口查询参数。
 */
@Data
public class BusinessAppQueryDTO {

    private String keyword;

    private String suiteCode;

    private String objectCode;

    private String appType;

    private String entryMode;

    private Integer status;
}
