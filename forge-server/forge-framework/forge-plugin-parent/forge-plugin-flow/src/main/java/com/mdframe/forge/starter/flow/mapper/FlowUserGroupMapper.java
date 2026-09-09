package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowUserGroup;
import com.mdframe.forge.starter.flow.vo.FlowUserGroupMemberVO;
import com.mdframe.forge.starter.flow.vo.FlowUserGroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 流程用户组及成员 Mapper。 */
@Mapper
public interface FlowUserGroupMapper extends BaseMapper<FlowUserGroup> {

    IPage<FlowUserGroupVO> selectPageByTenant(Page<FlowUserGroupVO> page,
                                               @Param("tenantId") Long tenantId,
                                               @Param("keyword") String keyword,
                                               @Param("groupCode") String groupCode,
                                               @Param("groupName") String groupName,
                                               @Param("status") Integer status);

    FlowUserGroupVO selectVoByIdAndTenant(@Param("id") Long id, @Param("tenantId") Long tenantId);

    FlowUserGroup selectByIdAndTenant(@Param("id") Long id, @Param("tenantId") Long tenantId);

    int countByCodeAndTenant(@Param("groupCode") String groupCode,
                             @Param("tenantId") Long tenantId,
                             @Param("excludeId") Long excludeId);

    int updateByIdAndTenant(@Param("group") FlowUserGroup group, @Param("tenantId") Long tenantId);

    int logicallyDeleteByIdAndTenant(@Param("id") Long id, @Param("tenantId") Long tenantId,
                                     @Param("operatorId") Long operatorId);

    int logicallyDeleteMembersByGroupId(@Param("groupId") Long groupId, @Param("tenantId") Long tenantId);

    int countActiveGroup(@Param("id") Long id, @Param("tenantId") Long tenantId);

    int countGroup(@Param("id") Long id, @Param("tenantId") Long tenantId);

    int insertMember(@Param("groupId") Long groupId, @Param("userId") Long userId,
                     @Param("tenantId") Long tenantId, @Param("operatorId") Long operatorId,
                     @Param("createDept") Long createDept);

    int logicallyDeleteMember(@Param("groupId") Long groupId, @Param("userId") Long userId,
                              @Param("tenantId") Long tenantId, @Param("operatorId") Long operatorId);

    List<FlowUserGroupMemberVO> selectMembers(@Param("groupId") Long groupId,
                                              @Param("tenantId") Long tenantId);

    List<Long> selectActiveUserIdsByCode(@Param("groupCode") String groupCode,
                                         @Param("tenantId") Long tenantId);

    List<String> selectActiveCodesByUserId(@Param("userId") Long userId,
                                           @Param("tenantId") Long tenantId);
}
