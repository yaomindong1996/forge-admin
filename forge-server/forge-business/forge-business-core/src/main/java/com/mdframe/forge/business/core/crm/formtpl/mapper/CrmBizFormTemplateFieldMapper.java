package com.mdframe.forge.business.core.crm.formtpl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.business.core.crm.formtpl.domain.CrmBizFormTemplateField;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CrmBizFormTemplateFieldMapper extends BaseMapper<CrmBizFormTemplateField> {
    int logicDeleteByTemplateId(@Param("tenantId") Long tenantId, @Param("templateId") Long templateId);
}
