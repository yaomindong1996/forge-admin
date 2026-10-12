package com.mdframe.forge.flow.controller;

import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.service.WorkspaceService;
import com.mdframe.forge.starter.flow.vo.WorkspaceSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 我的工作台聚合接口。
 */
@RestController
@RequestMapping("/api/flow/workspace")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    /**
     * 工作台首页聚合统计。
     */
    @GetMapping("/summary")
    public RespInfo<WorkspaceSummaryVO> summary() {
        return RespInfo.success(workspaceService.summary(currentUserId()));
    }

    /**
     * 顶部工作台待办徽标数。
     */
    @GetMapping("/todo-count")
    public RespInfo<Long> todoCount() {
        return RespInfo.success(workspaceService.todoCount(currentUserId()));
    }

    private String currentUserId() {
        Long userId = SessionHelper.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "当前用户未登录");
        }
        return String.valueOf(userId);
    }
}
