package com.mdframe.forge.business.core.crm.formtpl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.business.core.crm.formtpl.domain.CrmBizFormTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CrmBizFormTemplateMapper extends BaseMapper<CrmBizFormTemplate> {
    CrmBizFormTemplate selectDefaultTemplate(
            @Param("tenantId") Long tenantId,
            @Param("objectType") String objectType);
}
