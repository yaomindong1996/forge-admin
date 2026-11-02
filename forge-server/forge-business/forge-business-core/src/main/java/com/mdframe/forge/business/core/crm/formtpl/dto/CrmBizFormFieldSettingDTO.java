package com.mdframe.forge.business.core.crm.formtpl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrmBizFormFieldSettingDTO {
    /** 已有字段目录 ID；自定义新建时可空 */
    private Long fieldId;
    @NotBlank(message = "字段编码不能为空")
    private String fieldCode;
    @NotBlank(message = "字段类型不能为空")
    private String fieldKind;
    @NotBlank(message = "显示名不能为空")
    private String fieldLabel;
    @NotBlank(message = "组件类型不能为空")
    private String componentType;
    private String dictType;
    private String optionsJson;
    private String sectionCode;
    @NotNull(message = "启用标记不能为空")
    private Integer enabled;
    @NotNull(message = "必填标记不能为空")
    private Integer required;
    private Integer listVisible;
    private Integer listSortable;
    private Integer commonlyUsed;
    private String displayLabel;
    private Integer sortOrder;
    /** true 表示删除该自定义字段 */
    private Boolean remove;
}
