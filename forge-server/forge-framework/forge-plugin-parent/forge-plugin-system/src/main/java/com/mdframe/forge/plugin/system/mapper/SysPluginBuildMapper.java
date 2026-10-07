package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysPluginBuild;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface SysPluginBuildMapper extends BaseMapper<SysPluginBuild> {
    SysPluginBuild selectBuild(@Param("tenantId") Long tenantId, @Param("id") String id);
    SysPluginBuild selectSummary(@Param("tenantId") Long tenantId, @Param("id") String id);
    int heartbeat(@Param("build") SysPluginBuild build, @Param("previousPhase") String previousPhase,
                  @Param("now") LocalDateTime now);
    int finish(@Param("build") SysPluginBuild build, @Param("previousPhase") String previousPhase,
               @Param("now") LocalDateTime now);
}
