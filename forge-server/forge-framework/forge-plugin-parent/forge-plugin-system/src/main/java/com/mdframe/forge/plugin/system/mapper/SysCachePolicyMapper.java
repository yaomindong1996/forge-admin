package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysCachePolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysCachePolicyMapper extends BaseMapper<SysCachePolicy> {

    List<SysCachePolicy> selectActivePolicies(@Param("tenantId") Long tenantId);

    SysCachePolicy selectByIdentity(@Param("tenantId") Long tenantId,
                                    @Param("applicationCode") String applicationCode,
                                    @Param("cacheName") String cacheName);

    int updateWithVersion(@Param("policy") SysCachePolicy policy,
                          @Param("expectedVersion") Long expectedVersion);

    int logicalDeleteWithVersion(@Param("tenantId") Long tenantId,
                                 @Param("applicationCode") String applicationCode,
                                 @Param("cacheName") String cacheName,
                                 @Param("expectedVersion") Long expectedVersion,
                                 @Param("updateBy") Long updateBy,
                                 @Param("updateTime") LocalDateTime updateTime);
}
