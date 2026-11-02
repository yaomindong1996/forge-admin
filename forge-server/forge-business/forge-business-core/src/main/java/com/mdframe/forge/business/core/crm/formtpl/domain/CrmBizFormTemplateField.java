package com.mdframe.forge.business.core.crm.formtpl.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_biz_form_template_field")
public class CrmBizFormTemplateField extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private Long templateId;
    private Long fieldId;
    private Integer enabled;
    private Integer required;
    private Integer listVisible;
    private Integer listSortable;
    private Integer commonlyUsed;
    private String displayLabel;
    private Integer sortOrder;
    @TableLogic(value = "0", delval = "id") private Long delFlag;
}
