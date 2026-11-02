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
@TableName("crm_biz_form_field")
public class CrmBizFormField extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private String objectType;
    private String fieldCode;
    private String fieldKind;
    private String fieldLabel;
    private String componentType;
    private String dictType;
    private String optionsJson;
    private String sectionCode;
    private Integer locked;
    private Integer defaultEnabled;
    private Integer defaultRequired;
    private Integer sortOrder;
    private String remark;
    @TableLogic(value = "0", delval = "id") private Long delFlag;
}
