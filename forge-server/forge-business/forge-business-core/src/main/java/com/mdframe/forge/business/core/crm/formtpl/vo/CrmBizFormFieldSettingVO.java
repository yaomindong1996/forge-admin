package com.mdframe.forge.business.core.crm.formtpl.vo;

import lombok.Data;

@Data
public class CrmBizFormFieldSettingVO {
    private Long fieldId;
    private Long templateFieldId;
    private String fieldCode;
    private String fieldKind;
    private String fieldLabel;
    private String componentType;
    private String dictType;
    private String optionsJson;
    private String sectionCode;
    private Integer locked;
    private Integer enabled;
    private Integer required;
    private Integer listVisible;
    private Integer listSortable;
    private Integer commonlyUsed;
    private String displayLabel;
    private Integer sortOrder;
}
