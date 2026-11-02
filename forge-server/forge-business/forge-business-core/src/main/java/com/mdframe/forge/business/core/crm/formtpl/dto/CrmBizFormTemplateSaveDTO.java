package com.mdframe.forge.business.core.crm.formtpl.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CrmBizFormTemplateSaveDTO {
    @NotBlank(message = "业务对象不能为空")
    private String objectType;
    @NotNull(message = "版本号不能为空")
    private Integer version;
    private Integer formEnabled;
    private String templateName;
    @Valid
    private List<CrmBizFormFieldSettingDTO> fields = new ArrayList<>();
}
