package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowModelVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowModelVersionMapper extends BaseMapper<FlowModelVersion> {

    IPage<FlowModelVersion> pageByVersion(Page<FlowModelVersion> page, @Param("modelId") String modelId,
                                          @Param("tenantId") Long tenantId);

    Integer getMaxVersion(@Param("modelId") String modelId, @Param("tenantId") Long tenantId);

    FlowModelVersion getVersionDetail(@Param("versionId") String versionId, @Param("tenantId") Long tenantId);

    FlowModelVersion getVersionByModelAndVersion(@Param("modelId") String modelId, @Param("version") Integer version,
                                                 @Param("tenantId") Long tenantId);

    int updateVersionTagByIdAndTenant(@Param("versionId") String versionId,
                                      @Param("tenantId") Long tenantId,
                                      @Param("versionTag") String versionTag);

    int logicalDeleteByIdAndTenant(@Param("versionId") String versionId, @Param("tenantId") Long tenantId);

    /** 查询租户内可评估清理的历史版本，结果按新到旧稳定排序。 */
    List<FlowModelVersion> selectCleanupCandidates(@Param("modelId") String modelId,
                                                   @Param("tenantId") Long tenantId);
}
