package com.mdframe.forge.plugin.generator.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 业务统计 Mapper。
 */
@Mapper
public interface BusinessStatsMapper {

    Map<String, Object> selectOverview(@Param("tableName") String tableName,
                                       @Param("tenantId") Long tenantId);

    List<Map<String, Object>> selectGroupCount(@Param("tableName") String tableName,
                                               @Param("columnName") String columnName,
                                               @Param("tenantId") Long tenantId);

    List<Map<String, Object>> selectTrend(@Param("tableName") String tableName,
                                          @Param("tenantId") Long tenantId,
                                          @Param("period") String period,
                                          @Param("days") Integer days);

    Long selectSum(@Param("tableName") String tableName,
                   @Param("columnName") String columnName,
                   @Param("tenantId") Long tenantId);

    List<Map<String, Object>> selectFlowResultDistribution(@Param("tenantId") Long tenantId,
                                                           @Param("objectCode") String objectCode);
}
