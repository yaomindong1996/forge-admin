package com.mdframe.forge.business.core.crm.formtpl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.business.core.crm.formtpl.domain.CrmBizFormField;
import com.mdframe.forge.business.core.crm.formtpl.vo.CrmBizFormFieldSettingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CrmBizFormFieldMapper extends BaseMapper<CrmBizFormField> {
    List<CrmBizFormFieldSettingVO> selectTemplateFieldSettings(
            @Param("tenantId") Long tenantId,
            @Param("templateId") Long templateId);

    List<CrmBizFormFieldSettingVO> selectSchemeFields(
            @Param("tenantId") Long tenantId,
            @Param("templateId") Long templateId);

    CrmBizFormField selectByObjectAndCode(
            @Param("tenantId") Long tenantId,
            @Param("objectType") String objectType,
            @Param("fieldCode") String fieldCode);
}
