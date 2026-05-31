package com.mdframe.forge.plugin.generator.vo.businessapp;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务对象设计版本视图。
 */
@Data
public class BusinessObjectDesignVersionVO {

    private Long id;

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

    private Object designerOptionsSnapshot;

    private String publishStatus;

    private Integer publishVersion;

    private String remark;

    private LocalDateTime createTime;

    private Long createBy;
}
