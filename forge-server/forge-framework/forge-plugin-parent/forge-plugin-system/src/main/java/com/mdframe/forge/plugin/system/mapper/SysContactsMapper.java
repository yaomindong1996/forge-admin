package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.ContactMemberQuery;
import com.mdframe.forge.plugin.system.vo.ContactMemberLabelVO;
import com.mdframe.forge.plugin.system.vo.ContactMemberVO;
import com.mdframe.forge.plugin.system.vo.ContactOrgVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 通讯录只读查询，所有方法都只返回当前租户的有效成员和正常部门
 */
@Mapper
public interface SysContactsMapper {

    String selectTenantName(@Param("tenantId") Long tenantId);

    Long countMembers(@Param("tenantId") Long tenantId);

    ContactOrgVO selectMainOrg(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    List<ContactOrgVO> selectChildOrgs(@Param("tenantId") Long tenantId, @Param("parentId") Long parentId);

    IPage<ContactMemberVO> selectMemberPage(Page<ContactMemberVO> page,
                                            @Param("tenantId") Long tenantId,
                                            @Param("query") ContactMemberQuery query);

    ContactMemberVO selectMember(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    List<ContactMemberLabelVO> selectOrgNames(@Param("tenantId") Long tenantId,
                                              @Param("userIds") Collection<Long> userIds);

    List<ContactMemberLabelVO> selectPostNames(@Param("tenantId") Long tenantId,
                                               @Param("userIds") Collection<Long> userIds);
}
