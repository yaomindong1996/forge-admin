package com.mdframe.forge.business.core.crm.formtpl.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_biz_form_template")
public class CrmBizFormTemplate extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private String objectType;
    private String templateCode;
    private String templateName;
    private Integer isDefault;
    private Integer formEnabled;
    private String status;
    @Version private Integer version;
    private String remark;
    @TableLogic(value = "0", delval = "id") private Long delFlag;
}
