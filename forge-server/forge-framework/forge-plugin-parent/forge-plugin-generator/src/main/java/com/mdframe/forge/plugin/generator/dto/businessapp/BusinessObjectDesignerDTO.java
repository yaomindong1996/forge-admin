package com.mdframe.forge.plugin.generator.dto.businessapp;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 业务对象设计器保存参数。
 */
@Data
public class BusinessObjectDesignerDTO {

    private Long objectId;

    private String objectName;

    private String description;

    private String icon;

    private String displayField;

    private Integer status;

    private String designStatus;

    private LowcodeModelSchema modelSchema;

    private LowcodePageSchema pageSchema;

    private FormDesignerSchemaDTO formDesignerSchema;

    private ViewSchemaDTO viewSchema;

    private LinkageSchemaDTO linkageSchema;

    private List<BusinessFieldDTO> fields;

    private List<BusinessObjectRelationDTO> relations;

    private Map<String, Object> designerOptions;

    /**
     * @deprecated 保存设计与数据库同步已拆分；该兼容字段不再触发 DDL。
     */
    @Deprecated
    private Boolean syncDdl;

    /**
     * @deprecated 请改用独立 database-sync 接口显式确认。
     */
    @Deprecated
    private Boolean confirmSyncDdl;
}
