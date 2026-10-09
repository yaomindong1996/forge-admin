package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysPluginArtifactRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysPluginArtifactMapper extends BaseMapper<SysPluginArtifactRegistration> {
    SysPluginArtifactRegistration selectRequest(@Param("tenantId") Long tenantId, @Param("taskId") String taskId,
                                                @Param("actorId") Long actorId, @Param("requestId") String requestId);
    List<SysPluginArtifactRegistration> selectTask(@Param("tenantId") Long tenantId, @Param("taskId") String taskId);
}
