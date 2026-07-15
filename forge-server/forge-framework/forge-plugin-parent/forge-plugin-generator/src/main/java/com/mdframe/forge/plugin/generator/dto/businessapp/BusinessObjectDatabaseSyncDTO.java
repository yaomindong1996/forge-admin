package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 业务对象数据库差异预览与显式同步参数。
 */
@Data
public class BusinessObjectDatabaseSyncDTO {

    /** 当前对象设计草稿版本。 */
    private Integer designVersion;

    /** 在线执行 DDL 的显式二次确认。 */
    private Boolean confirmOnlineDdl;
}
