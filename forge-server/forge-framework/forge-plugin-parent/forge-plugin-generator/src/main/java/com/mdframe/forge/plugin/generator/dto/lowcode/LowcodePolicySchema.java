package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码业务对象策略协议。
 */
@Data
public class LowcodePolicySchema {

    private String dataScope = "TENANT";

    private String userField;

    private String userColumn;

    private String orgField;

    private String orgColumn;

    private String regionField;

    private String regionColumn;

    /**
     * 流程经手人可查看单据。null 视为开启。
     */
    private Boolean flowRelatedVisible;

    private Boolean auditEnabled = true;

    /** 主键策略：低代码业务表固定使用 id 自增主键。 */
    private String primaryKeyStrategy = "AUTO_INCREMENT";

    private String primaryKeyField = "id";

    private String tenantField = "tenantId";

    private String tenantColumn = "tenant_id";

    private String logicDeleteField = "delFlag";

    private String logicDeleteColumn = "del_flag";
}
