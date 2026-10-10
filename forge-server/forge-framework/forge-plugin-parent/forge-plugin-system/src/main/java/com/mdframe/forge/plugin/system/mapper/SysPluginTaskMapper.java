package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysPluginTaskQuery;
import com.mdframe.forge.plugin.system.entity.SysPluginTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysPluginTaskMapper extends BaseMapper<SysPluginTask> {
    IPage<SysPluginTask> selectTaskPage(Page<SysPluginTask> page, @Param("tenantId") Long tenantId,
                                      @Param("query") SysPluginTaskQuery query);
    SysPluginTask selectTask(@Param("tenantId") Long tenantId, @Param("id") String id);
    SysPluginTask lockTask(@Param("tenantId") Long tenantId, @Param("id") String id);
    SysPluginTask selectByRequest(@Param("tenantId") Long tenantId, @Param("actorId") Long actorId,
                                  @Param("requestId") String requestId);
    SysPluginTask selectArchive(@Param("tenantId") Long tenantId, @Param("id") String id);
    int transition(@Param("task") SysPluginTask task, @Param("expectedStatus") String expectedStatus,
                   @Param("expectedRevision") int expectedRevision);
}
