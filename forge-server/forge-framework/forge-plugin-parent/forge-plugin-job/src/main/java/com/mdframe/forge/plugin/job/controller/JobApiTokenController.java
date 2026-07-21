package com.mdframe.forge.plugin.job.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.job.constant.JobPermissions;
import com.mdframe.forge.plugin.job.dto.JobApiTokenCreateRequest;
import com.mdframe.forge.plugin.job.service.JobApiTokenService;
import com.mdframe.forge.plugin.job.vo.JobApiResourceOptionVO;
import com.mdframe.forge.plugin.job.vo.JobApiTokenCreatedVO;
import com.mdframe.forge.plugin.job.vo.JobApiTokenVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/job/api-token")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ConditionalOnProperty(
        prefix = "forge.job.open-api", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JobApiTokenController {

    private final JobApiTokenService tokenService;

    @GetMapping("/page")
    @SaCheckPermission(JobPermissions.API_TOKEN_LIST)
    public RespInfo<Page<JobApiTokenVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String callerName,
            @RequestParam(required = false) String status) {
        return RespInfo.success(tokenService.page(
                SessionHelper.getTenantId(), pageNum, pageSize, callerName, status));
    }

    @GetMapping("/resources")
    @SaCheckPermission(JobPermissions.API_TOKEN_LIST)
    public RespInfo<List<JobApiResourceOptionVO>> resources() {
        return RespInfo.success(tokenService.listResourceOptions());
    }

    @PostMapping
    @SaCheckPermission(JobPermissions.API_TOKEN_ADD)
    @OperationLog(module = "定时任务开放API", type = OperationType.ADD, desc = "创建开放API服务账号",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<JobApiTokenCreatedVO> create(@Valid @RequestBody JobApiTokenCreateRequest request) {
        return RespInfo.success(tokenService.create(SessionHelper.getTenantId(), request));
    }

    @PostMapping("/{id}/revoke")
    @SaCheckPermission(JobPermissions.API_TOKEN_REVOKE)
    @OperationLog(module = "定时任务开放API", type = OperationType.UPDATE, desc = "吊销开放API服务账号",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> revoke(@PathVariable Long id) {
        tokenService.revoke(SessionHelper.getTenantId(), id);
        return RespInfo.success();
    }

    @PostMapping("/{id}/rotate")
    @SaCheckPermission(JobPermissions.API_TOKEN_ROTATE)
    @OperationLog(module = "定时任务开放API", type = OperationType.UPDATE, desc = "轮换开放API服务账号",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<JobApiTokenCreatedVO> rotate(@PathVariable Long id) {
        return RespInfo.success(tokenService.rotate(SessionHelper.getTenantId(), id));
    }
}
