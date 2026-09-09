package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.starter.flow.entity.FlowCc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 流程抄送 Mapper
 */
@Mapper
public interface FlowCcMapper extends BaseMapper<FlowCc> {

    /**
     * 按租户和接收人分页查询有效抄送。查询条件必须留在 XML，避免绕过数据权限审查。
     */
    IPage<FlowCc> selectMyPage(IPage<FlowCc> page,
                               @Param("userId") String userId,
                               @Param("tenantId") Long tenantId,
                               @Param("isRead") Integer isRead,
                               @Param("title") String title);

    /** 按租户和发送人分页查询抄送（发送人可查看已撤回关系）。 */
    IPage<FlowCc> selectSentPage(IPage<FlowCc> page,
                                 @Param("userId") String userId,
                                 @Param("tenantId") Long tenantId,
                                 @Param("title") String title);

    /**
     * 统计工作台未读抄送数。
     */
    Long countWorkspaceUnread(@Param("userId") String userId, @Param("tenantId") Long tenantId);

    int countVisibleByProcess(@Param("processInstanceId") String processInstanceId,
                              @Param("userId") String userId,
                              @Param("tenantId") Long tenantId);

    int deleteByProcessInstanceIdPhysically(@Param("processInstanceId") String processInstanceId,
                                            @Param("tenantId") Long tenantId);

    int revokeBySender(@Param("id") String id, @Param("tenantId") Long tenantId,
                       @Param("senderId") String senderId, @Param("reason") String reason,
                       @Param("revokeTime") java.time.LocalDateTime revokeTime,
                       @Param("status") Integer status);

    int markAllRead(@Param("userId") String userId, @Param("tenantId") Long tenantId,
                    @Param("readTime") java.time.LocalDateTime readTime);
}
