package com.mdframe.forge.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.dto.FlowUserGroupCreateDTO;
import com.mdframe.forge.starter.flow.dto.FlowUserGroupMembersDTO;
import com.mdframe.forge.starter.flow.dto.FlowUserGroupQuery;
import com.mdframe.forge.starter.flow.dto.FlowUserGroupUpdateDTO;
import com.mdframe.forge.starter.flow.service.FlowUserGroupService;
import com.mdframe.forge.starter.flow.vo.FlowUserGroupMemberVO;
import com.mdframe.forge.starter.flow.vo.FlowUserGroupVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 流程用户组管理接口。 */
@RestController
@RequestMapping("/api/flow/org/groups")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowUserGroupController {

    private final FlowUserGroupService flowUserGroupService;

    @GetMapping("/page")
    @SaCheckPermission("flow:org:group:view")
    public RespInfo<IPage<FlowUserGroupVO>> page(FlowUserGroupQuery query) {
        return RespInfo.success(flowUserGroupService.page(query));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("flow:org:group:view")
    public RespInfo<FlowUserGroupVO> getById(@PathVariable Long id) {
        return RespInfo.success(flowUserGroupService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("flow:org:group:manage")
    public RespInfo<FlowUserGroupVO> create(@Valid @RequestBody FlowUserGroupCreateDTO request) {
        return RespInfo.success("创建成功", flowUserGroupService.create(request));
    }

    @PutMapping
    @SaCheckPermission("flow:org:group:manage")
    public RespInfo<FlowUserGroupVO> update(@Valid @RequestBody FlowUserGroupUpdateDTO request) {
        return RespInfo.success("更新成功", flowUserGroupService.update(request));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("flow:org:group:manage")
    public RespInfo<Void> delete(@PathVariable Long id) {
        flowUserGroupService.delete(id);
        return RespInfo.success();
    }

    @GetMapping("/{id}/members")
    @SaCheckPermission("flow:org:group:view")
    public RespInfo<List<FlowUserGroupMemberVO>> listMembers(@PathVariable Long id) {
        return RespInfo.success(flowUserGroupService.listMembers(id));
    }

    @PostMapping("/{id}/members")
    @SaCheckPermission("flow:org:group:manage")
    public RespInfo<Void> addMembers(@PathVariable Long id,
                                     @Valid @RequestBody FlowUserGroupMembersDTO request) {
        flowUserGroupService.addMembers(id, request);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}/members")
    @SaCheckPermission("flow:org:group:manage")
    public RespInfo<Void> removeMembers(@PathVariable Long id,
                                        @Valid @RequestBody FlowUserGroupMembersDTO request) {
        flowUserGroupService.removeMembers(id, request);
        return RespInfo.success();
    }
}
