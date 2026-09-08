package com.mdframe.forge.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.dto.FlowCommentPhraseCreateDTO;
import com.mdframe.forge.starter.flow.dto.FlowCommentPhraseQuery;
import com.mdframe.forge.starter.flow.dto.FlowCommentPhraseUpdateDTO;
import com.mdframe.forge.starter.flow.service.FlowCommentPhraseService;
import com.mdframe.forge.starter.flow.vo.FlowCommentPhraseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 常用审批意见：审批点选和个人维护走登录态，企业管理走独立权限。 */
@RestController
@RequestMapping("/api/flow/comment-phrases")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowCommentPhraseController {

    private final FlowCommentPhraseService flowCommentPhraseService;

    @GetMapping("/usable")
    @ApiPermissionIgnore
    public RespInfo<List<FlowCommentPhraseVO>> usable(@RequestParam(required = false) String scene) {
        return RespInfo.success(flowCommentPhraseService.listUsable(scene));
    }

    @GetMapping("/mine")
    @ApiPermissionIgnore
    public RespInfo<List<FlowCommentPhraseVO>> mine() {
        return RespInfo.success(flowCommentPhraseService.listMine());
    }

    @GetMapping("/page")
    @SaCheckPermission("flow:comment-phrase:view")
    public RespInfo<IPage<FlowCommentPhraseVO>> page(FlowCommentPhraseQuery query) {
        return RespInfo.success(flowCommentPhraseService.page(query));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("flow:comment-phrase:view")
    public RespInfo<FlowCommentPhraseVO> getById(@PathVariable Long id) {
        return RespInfo.success(flowCommentPhraseService.getById(id));
    }

    @PostMapping
    @ApiPermissionIgnore
    public RespInfo<FlowCommentPhraseVO> create(@Valid @RequestBody FlowCommentPhraseCreateDTO request) {
        return RespInfo.success("创建成功", flowCommentPhraseService.create(request));
    }

    @PutMapping
    @ApiPermissionIgnore
    public RespInfo<FlowCommentPhraseVO> update(@Valid @RequestBody FlowCommentPhraseUpdateDTO request) {
        return RespInfo.success("更新成功", flowCommentPhraseService.update(request));
    }

    @DeleteMapping("/{id}")
    @ApiPermissionIgnore
    public RespInfo<Void> delete(@PathVariable Long id) {
        flowCommentPhraseService.delete(id);
        return RespInfo.success();
    }
}
