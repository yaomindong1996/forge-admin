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

    /** true 表示保存业务对象设计后同步缺失数据表结构。 */
    private Boolean syncDdl;

    /** 同步表结构二次确认标记。 */
    private Boolean confirmSyncDdl;
}
