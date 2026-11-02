package com.mdframe.forge.business.core.crm.formtpl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SysBizFormExtSaveDTO {
    @NotBlank(message = "业务对象不能为空")
    private String objectType;
    @NotNull(message = "业务主键不能为空")
    private Long bizId;
    /** 自定义字段 JSON 对象字符串，允许为空对象 */
    private String extJson;
}
