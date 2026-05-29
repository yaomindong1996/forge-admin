package com.mdframe.forge.plugin.generator.dto.businessapp;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    private List<BusinessFieldDTO> fields = new ArrayList<>();

    private List<BusinessObjectRelationDTO> relations = new ArrayList<>();

    private Map<String, Object> designerOptions = new LinkedHashMap<>();
}
