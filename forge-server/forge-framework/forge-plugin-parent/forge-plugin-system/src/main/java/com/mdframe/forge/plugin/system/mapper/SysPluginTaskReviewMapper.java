package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysPluginTaskReview;
import com.mdframe.forge.plugin.system.service.plugin.PluginReleaseApprovalSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysPluginTaskReviewMapper extends BaseMapper<SysPluginTaskReview> {
    SysPluginTaskReview selectRequest(@Param("tenantId") Long tenantId, @Param("taskId") String taskId,
                                     @Param("actorId") Long actorId, @Param("requestId") String requestId);
    List<SysPluginTaskReview> selectRecent(@Param("tenantId") Long tenantId, @Param("taskId") String taskId);
    PluginReleaseApprovalSnapshot selectApprovalSnapshot(@Param("tenantId") Long tenantId,
            @Param("taskId") String taskId, @Param("reviewId") String reviewId);
}
