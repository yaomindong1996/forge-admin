package com.mdframe.forge.business.core.crm.formtpl.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CrmBizFormTemplateDetailVO {
    private Long id;
    private String objectType;
    private String templateCode;
    private String templateName;
    private Integer isDefault;
    private Integer formEnabled;
    private String status;
    private Integer version;
    private List<CrmBizFormFieldSettingVO> fields = new ArrayList<>();
}
