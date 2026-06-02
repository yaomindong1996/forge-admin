package com.mdframe.forge.plugin.generator.vo.businessapp;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.businessapp.FormDesignerSchemaDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.LinkageSchemaDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.ViewSchemaDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务对象设计器聚合视图。
 */
@Data
public class BusinessObjectDesignerVO {

    private Long objectId;

    private String suiteCode;

    private String suiteName;

    private String objectCode;

    private String objectName;

    private String objectType;

    private String displayField;

    private String icon;

    private String description;

    private Integer status;

    private String designStatus;

    private String publishStatus;

    private Boolean hasUnpublishedChanges;

    private Integer lastPublishVersion;

    private LocalDateTime lastPublishTime;

    private LocalDateTime updateTime;

    private LowcodeModelSchema modelSchema;

    private LowcodePageSchema pageSchema;

    private FormDesignerSchemaDTO formDesignerSchema;

    private ViewSchemaDTO viewSchema;

    private LinkageSchemaDTO linkageSchema;

    private BusinessDocumentConfigVO documentConfig;

    private List<BusinessFieldVO> fields = new ArrayList<>();

    private List<BusinessObjectRelationVO> relations = new ArrayList<>();

    private Map<String, Object> designerOptions = new LinkedHashMap<>();
}
