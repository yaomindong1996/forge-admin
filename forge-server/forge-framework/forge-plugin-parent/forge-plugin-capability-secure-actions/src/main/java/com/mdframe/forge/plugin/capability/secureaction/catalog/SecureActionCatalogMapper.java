package com.mdframe.forge.plugin.capability.secureaction.catalog;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SecureActionCatalogMapper {

    List<SecureActionCatalogRow> selectGrantedActions(@Param("tenantId") Long tenantId,
                                                      @Param("clientId") Long clientId,
                                                      @Param("keyword") String keyword,
                                                      @Param("afterCode") String afterCode,
                                                      @Param("afterId") Long afterId,
                                                      @Param("limit") Integer limit);

    SecureActionCatalogRow selectGrantedAction(@Param("tenantId") Long tenantId,
                                               @Param("clientId") Long clientId,
                                               @Param("capabilityCode") String capabilityCode);
}
