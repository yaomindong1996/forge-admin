package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码数据模型保存请求。
 */
@Data
public class LowcodeDataModelDTO {

    private Long id;

    private Long domainId;

    private String modelCode;

    private String modelName;

    private String modelDesc;

    private String status;

    private Boolean tenantEnabled;

    private Boolean masterData;

    private LowcodeModelSchema modelSchema;

    /** 从业务对象中心进入时用于回写 ai_business_object。 */
    private String businessSuiteCode;

    /** 从业务对象中心进入时用于回写 ai_business_object。 */
    private String businessObjectCode;

    /** true 表示保存模型配置后同步表结构；false/null 表示仅保存模型配置。 */
    private Boolean syncDdl;

    /** 同步表结构二次确认标记。 */
    private Boolean confirmSyncDdl;
}
