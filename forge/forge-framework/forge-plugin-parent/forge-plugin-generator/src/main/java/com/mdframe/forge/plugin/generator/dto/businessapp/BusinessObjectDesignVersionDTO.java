package com.mdframe.forge.plugin.generator.dto.businessapp;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import lombok.Data;

/**
 * 业务对象设计版本保存参数。
 */
@Data
public class BusinessObjectDesignVersionDTO {

    private Long objectId;

    private String suiteCode;

    private String objectCode;

    private Long configId;

    private String configKey;

    private Long crudConfigVersionId;

    private Integer versionNo;

    private String versionType;

    private LowcodeModelSchema modelSnapshot;

    private LowcodePageSchema pageSnapshot;

    private Object relationSnapshot;

    private String publishStatus;

    private Integer publishVersion;

    private String remark;
}
