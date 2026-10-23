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

    /**
     * 强制执行预览中的全部 DDL（含改类型族、缩短长度等）。
     * 必须与 {@link #confirmOnlineDdl}=true 同时传入；仍禁止 DROP/TRUNCATE。
     */
    private Boolean forceExecute;
}
