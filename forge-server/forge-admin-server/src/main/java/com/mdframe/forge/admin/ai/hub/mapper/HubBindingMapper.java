package com.mdframe.forge.admin.ai.hub.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HubBindingMapper {

    String selectBindingValue(@Param("tenantId") Long tenantId,
                              @Param("bindingKind") String bindingKind);
}
