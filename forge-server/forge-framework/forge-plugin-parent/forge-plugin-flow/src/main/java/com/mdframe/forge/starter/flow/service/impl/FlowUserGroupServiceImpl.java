package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.dto.FlowUserGroupCreateDTO;
import com.mdframe.forge.starter.flow.dto.FlowUserGroupMembersDTO;
import com.mdframe.forge.starter.flow.dto.FlowUserGroupQuery;
import com.mdframe.forge.starter.flow.dto.FlowUserGroupUpdateDTO;
import com.mdframe.forge.starter.flow.entity.FlowUserGroup;
import com.mdframe.forge.starter.flow.mapper.FlowUserGroupMapper;
import com.mdframe.forge.starter.flow.service.FlowUserGroupService;
import com.mdframe.forge.starter.flow.vo.FlowUserGroupMemberVO;
import com.mdframe.forge.starter.flow.vo.FlowUserGroupVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/** 默认流程用户组实现，所有管理和解析均绑定当前租户。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowUserGroupServiceImpl implements FlowUserGroupService {

    private static final int MAX_MEMBERS = 200;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_CODE_LENGTH = 100;

    private final FlowUserGroupMapper groupMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public IPage<FlowUserGroupVO> page(FlowUserGroupQuery query) {
        FlowUserGroupQuery safeQuery = query == null ? new FlowUserGroupQuery() : query;
        int pageNum = safeQuery.getPageNum();
        int pageSize = Math.min(safeQuery.getPageSize(), MAX_PAGE_SIZE);
        Page<FlowUserGroupVO> page = new Page<>(pageNum, pageSize);
        return groupMapper.selectPageByTenant(page, requireTenantId(), trimToNull(safeQuery.getKeyword()),
                trimToNull(safeQuery.getGroupCode()), trimToNull(safeQuery.getGroupName()), safeQuery.getStatus());
    }

    @Override
    public FlowUserGroupVO getById(Long id) {
        validateId(id);
        FlowUserGroupVO group = groupMapper.selectVoByIdAndTenant(id, requireTenantId());
        if (group == null) {
            throw notFound();
        }
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowUserGroupVO create(FlowUserGroupCreateDTO request) {
        Long tenantId = requireTenantId();
        validateCreate(request);
        String code = request.getGroupCode().trim();
        if (groupMapper.countByCodeAndTenant(code, tenantId, null) > 0) {
            throw new BusinessException(409, "用户组编码已存在");
        }
        FlowUserGroup group = new FlowUserGroup();
        group.setTenantId(tenantId);
        group.setGroupCode(code);
        group.setGroupName(request.getGroupName().trim());
        group.setStatus(request.getStatus() == null ? EnableStatus.ENABLED.getCode() : request.getStatus());
        group.setRemark(trimToNull(request.getRemark()));
        group.setCreateBy(SessionHelper.getUserId());
        group.setCreateDept(SessionHelper.getActiveOrgId());
        group.setCreateTime(LocalDateTime.now());
        groupMapper.insert(group);
        log.info("创建流程用户组: tenantId={}, groupId={}", tenantId, group.getId());
        return getById(group.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowUserGroupVO update(FlowUserGroupUpdateDTO request) {
        Long tenantId = requireTenantId();
        validateUpdate(request);
        FlowUserGroup existing = groupMapper.selectByIdAndTenant(request.getId(), tenantId);
        if (existing == null) {
            throw notFound();
        }
        FlowUserGroup patch = new FlowUserGroup();
        patch.setId(existing.getId());
        patch.setGroupName(request.getGroupName().trim());
        patch.setStatus(request.getStatus() == null ? existing.getStatus() : request.getStatus());
        patch.setRemark(trimToNull(request.getRemark()));
        patch.setUpdateBy(SessionHelper.getUserId());
        patch.setUpdateTime(LocalDateTime.now());
        if (groupMapper.updateByIdAndTenant(patch, tenantId) == 0) {
            throw notFound();
        }
        return getById(request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        validateId(id);
        if (groupMapper.countGroup(id, tenantId) == 0) {
            throw notFound();
        }
        Long operatorId = SessionHelper.getUserId();
        groupMapper.logicallyDeleteMembersByGroupId(id, tenantId);
        if (groupMapper.logicallyDeleteByIdAndTenant(id, tenantId, operatorId) == 0) {
            throw notFound();
        }
        log.info("删除流程用户组: tenantId={}, groupId={}, operatorId={}", tenantId, id, operatorId);
    }

    @Override
    public List<FlowUserGroupMemberVO> listMembers(Long id) {
        Long tenantId = requireTenantId();
        validateId(id);
        ensureGroupExists(id, tenantId);
        return groupMapper.selectMembers(id, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMembers(Long id, FlowUserGroupMembersDTO request) {
        Long tenantId = requireTenantId();
        ensureActiveGroup(id, tenantId);
        List<Long> userIds = normalizeUserIds(request);
        List<Long> available = sysUserMapper.selectFlowAvailableUserIds(tenantId, userIds);
        if (available.size() != userIds.size()) {
            throw new BusinessException(400, "只能添加当前租户内启用用户");
        }
        Long operatorId = SessionHelper.getUserId();
        for (Long userId : userIds) {
            groupMapper.insertMember(id, userId, tenantId, operatorId, SessionHelper.getActiveOrgId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMembers(Long id, FlowUserGroupMembersDTO request) {
        Long tenantId = requireTenantId();
        ensureActiveGroup(id, tenantId);
        List<Long> userIds = normalizeUserIds(request);
        Long operatorId = SessionHelper.getUserId();
        for (Long userId : userIds) {
            groupMapper.logicallyDeleteMember(id, userId, tenantId, operatorId);
        }
    }

    @Override
    public List<String> resolveUserIdsByCode(String groupCode) {
        Long tenantId = resolveTenantId();
        String code = trimToNull(groupCode);
        if (tenantId == null || tenantId <= 0 || code == null) {
            return Collections.emptyList();
        }
        return groupMapper.selectActiveUserIdsByCode(code, tenantId).stream()
                .limit(MAX_MEMBERS)
                .map(String::valueOf)
                .toList();
    }

    @Override
    public List<String> resolveGroupCodesByUserId(Long userId) {
        Long tenantId = resolveTenantId();
        if (tenantId == null || tenantId <= 0 || userId == null || userId <= 0) {
            return Collections.emptyList();
        }
        return groupMapper.selectActiveCodesByUserId(userId, tenantId);
    }

    private List<Long> normalizeUserIds(FlowUserGroupMembersDTO request) {
        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long userId : request.getUserIds()) {
            if (userId == null || userId <= 0) {
                throw new BusinessException(400, "用户ID不合法");
            }
            unique.add(userId);
        }
        if (unique.size() > MAX_MEMBERS) {
            throw new BusinessException(400, "单次最多维护200名成员");
        }
        return List.copyOf(unique);
    }

    private void validateCreate(FlowUserGroupCreateDTO request) {
        if (request == null || !StringUtils.hasText(request.getGroupCode())
                || request.getGroupCode().trim().length() > MAX_CODE_LENGTH
                || !request.getGroupCode().trim().matches("[A-Za-z0-9][A-Za-z0-9_.:-]*")) {
            throw new BusinessException(400, "用户组编码格式不合法");
        }
        if (!StringUtils.hasText(request.getGroupName()) || request.getGroupName().trim().length() > 100) {
            throw new BusinessException(400, "用户组名称不能为空且长度不能超过100");
        }
        validateStatus(request.getStatus());
    }

    private void validateUpdate(FlowUserGroupUpdateDTO request) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new BusinessException(400, "用户组ID不合法");
        }
        if (!StringUtils.hasText(request.getGroupName()) || request.getGroupName().trim().length() > 100) {
            throw new BusinessException(400, "用户组名称不能为空且长度不能超过100");
        }
        validateStatus(request.getStatus());
    }

    private void validateStatus(Integer status) {
        if (status != null && !EnableStatus.ENABLED.matches(status) && !EnableStatus.DISABLED.matches(status)) {
            throw new BusinessException(400, "用户组状态不合法");
        }
    }

    private void ensureActiveGroup(Long id, Long tenantId) {
        validateId(id);
        if (groupMapper.countActiveGroup(id, tenantId) == 0) {
            throw notFound();
        }
    }

    private void ensureGroupExists(Long id, Long tenantId) {
        validateId(id);
        if (groupMapper.countGroup(id, tenantId) == 0) {
            throw notFound();
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "用户组ID不合法");
        }
    }

    private Long requireTenantId() {
        Long tenantId = resolveTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException(403, "无法确定当前租户，禁止管理流程用户组");
        }
        return tenantId;
    }

    private Long resolveTenantId() {
        return SessionHelper.getTenantId();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BusinessException notFound() {
        return new BusinessException(404, "用户组不存在");
    }
}
