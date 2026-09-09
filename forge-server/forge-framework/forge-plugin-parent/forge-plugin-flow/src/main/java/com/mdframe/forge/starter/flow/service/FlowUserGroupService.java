package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.starter.flow.dto.FlowUserGroupCreateDTO;
import com.mdframe.forge.starter.flow.dto.FlowUserGroupMembersDTO;
import com.mdframe.forge.starter.flow.dto.FlowUserGroupQuery;
import com.mdframe.forge.starter.flow.dto.FlowUserGroupUpdateDTO;
import com.mdframe.forge.starter.flow.vo.FlowUserGroupMemberVO;
import com.mdframe.forge.starter.flow.vo.FlowUserGroupVO;

import java.util.List;

/** 流程用户组管理与运行时解析服务。 */
public interface FlowUserGroupService {

    IPage<FlowUserGroupVO> page(FlowUserGroupQuery query);

    FlowUserGroupVO getById(Long id);

    FlowUserGroupVO create(FlowUserGroupCreateDTO request);

    FlowUserGroupVO update(FlowUserGroupUpdateDTO request);

    void delete(Long id);

    List<FlowUserGroupMemberVO> listMembers(Long id);

    void addMembers(Long id, FlowUserGroupMembersDTO request);

    void removeMembers(Long id, FlowUserGroupMembersDTO request);

    /** 根据 BPMN candidateGroups 编码解析租户内当前有效用户。 */
    List<String> resolveUserIdsByCode(String groupCode);

    /** 返回当前用户所属的自定义用户组编码，用于任务可见性。 */
    List<String> resolveGroupCodesByUserId(Long userId);
}
