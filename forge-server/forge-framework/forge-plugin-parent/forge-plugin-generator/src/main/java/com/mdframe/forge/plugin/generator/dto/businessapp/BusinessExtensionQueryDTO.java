package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 业务扩展分页筛选参数。
 */
@Data
public class BusinessExtensionQueryDTO {

    private Long applicationId;

    private Long objectId;

    private Long entryId;

    private String keyword;

    private String extensionType;

    private String hookCode;

    private String status;
}
