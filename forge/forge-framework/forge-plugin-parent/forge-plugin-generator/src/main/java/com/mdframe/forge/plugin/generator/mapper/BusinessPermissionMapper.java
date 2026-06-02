package com.mdframe.forge.plugin.generator.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 业务权限辅助 Mapper。
 */
@Mapper
public interface BusinessPermissionMapper {

    List<String> selectExistingPermissions(@Param("tenantId") Long tenantId,
                                           @Param("permissions") List<String> permissions);
}
