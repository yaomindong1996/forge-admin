package com.mdframe.forge.business.core.crm.formtpl.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CrmBizFormSchemeVO {
    private String objectType;
    private Long templateId;
    private String templateCode;
    private String templateName;
    private Integer formEnabled;
    private Integer version;
    private List<CrmBizFormFieldSettingVO> fields = new ArrayList<>();
}
