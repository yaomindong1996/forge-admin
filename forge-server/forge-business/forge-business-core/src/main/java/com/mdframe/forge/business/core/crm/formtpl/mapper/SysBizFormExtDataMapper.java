package com.mdframe.forge.business.core.crm.formtpl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.business.core.crm.formtpl.domain.SysBizFormExtData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysBizFormExtDataMapper extends BaseMapper<SysBizFormExtData> {

    @Select("""
            SELECT * FROM sys_biz_form_ext_data
            WHERE tenant_id = #{tenantId}
              AND object_type = #{objectType}
              AND biz_id = #{bizId}
              AND del_flag = 0
            LIMIT 1
            """)
    SysBizFormExtData selectByObjectAndBizId(@Param("tenantId") Long tenantId,
                                             @Param("objectType") String objectType,
                                             @Param("bizId") Long bizId);
}
