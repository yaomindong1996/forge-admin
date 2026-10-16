package com.mdframe.forge.plugin.collaboration.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.collaboration.domain.model.DirectorySyncResult;
import com.mdframe.forge.plugin.collaboration.dto.CollaborationAppSaveRequest;
import com.mdframe.forge.plugin.collaboration.dto.CollaborationConnectionSaveRequest;
import com.mdframe.forge.plugin.collaboration.dto.CollaborationSyncCommand;
import com.mdframe.forge.plugin.collaboration.job.CollaborationSyncJobManager;
import com.mdframe.forge.plugin.collaboration.service.directory.DirectorySyncOrchestrator;
import com.mdframe.forge.plugin.collaboration.vo.CollaborationConnectionVO;
import com.mdframe.forge.starter.collaboration.CollaborationCapability;
import com.mdframe.forge.starter.collaboration.connector.AccessTokenProvider;
import com.mdframe.forge.starter.collaboration.model.CollaborationExecutionContext;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.social.domain.entity.SysSocialAppConfig;
import com.mdframe.forge.starter.social.domain.entity.SysSocialConfig;
import com.mdframe.forge.starter.social.domain.vo.SocialConnectionSummaryVO;
import com.mdframe.forge.starter.social.service.ISocialAppConfigService;
import com.mdframe.forge.starter.social.service.ISocialConfigService;
import com.mdframe.forge.plugin.collaboration.support.CollaborationTenantHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 企业协同连接与应用管理控制器（Task 18）。
 * <p>
 * 读接口统一返回脱敏 VO，凭据只体现「是否已配置 + 固定掩码」；
 * 连接接口不接收任何明文凭据，凭据写入统一走应用管理接口并由服务层加密落库。
 */
@RestController
@RequestMapping("/system/collaboration/connections")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class CollaborationConnectionController {

    private final ISocialConfigService socialConfigService;
    private final ISocialAppConfigService appConfigService;
    private final DirectorySyncOrchestrator syncOrchestrator;
    private final List<AccessTokenProvider> accessTokenProviders;
    /**
     * 定时同步托管器：仅在部署了调度器的服务（forge-plugin-job 存在）时可用；
     * app-server 等不启调度器的服务此 Bean 缺失，用 ObjectProvider 兜底避免注入失败。
     */
    private final ObjectProvider<CollaborationSyncJobManager> syncJobManager;

    /**
     * 分页查询连接列表（凭据脱敏）
     */
    @GetMapping("/page")
    @SaCheckPermission("system:collaboration:connection:list")
    @OperationLog(module = "企业协同连接", type = OperationType.QUERY, desc = "分页查询连接列表")
    public RespInfo<Page<SocialConnectionSummaryVO>> page(PageQuery pageQuery, SysSocialConfig query) {
        Page<SysSocialConfig> page = socialConfigService.selectConfigPage(pageQuery.toPage(), query);
        Page<SocialConnectionSummaryVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(SocialConnectionSummaryVO::from).toList());
        return RespInfo.success(voPage);
    }

    /**
     * 连接详情：组合连接摘要、物理应用与能力绑定
     */
    @GetMapping("/{id}")
    @SaCheckPermission("system:collaboration:connection:list")
    @OperationLog(module = "企业协同连接", type = OperationType.QUERY, desc = "查询连接详情")
    public RespInfo<CollaborationConnectionVO> detail(@PathVariable Long id) {
        SysSocialConfig connection = requireConnection(id);
        Long tenantId = CollaborationTenantHelper.currentTenantId();
        CollaborationConnectionVO vo = new CollaborationConnectionVO();
        vo.setConnection(SocialConnectionSummaryVO.from(connection));
        vo.setApps(appConfigService.listApps(tenantId, id).stream()
                .map(app -> CollaborationConnectionVO.AppVO.from(app, appConfigService.secretSummary(app)))
                .toList());
        vo.setBindings(appConfigService.listBindings(tenantId, id).stream()
                .map(CollaborationConnectionVO.CapabilityBindingVO::from)
                .toList());
        return RespInfo.success(vo);
    }

    /**
     * 新增连接（不接收凭据字段）
     */
    @PostMapping
    @SaCheckPermission("system:collaboration:connection:create")
    @OperationLog(module = "企业协同连接", type = OperationType.ADD, desc = "新增连接")
    public RespInfo<Void> create(@Valid @RequestBody CollaborationConnectionSaveRequest request) {
        SysSocialConfig entity = request.toEntity();
        // 超管请求处于租户忽略态，MP 租户插件不会自动注入 tenant_id，需显式兜底
        if (entity.getTenantId() == null) {
            entity.setTenantId(CollaborationTenantHelper.currentTenantId());
        }
        boolean result = socialConfigService.insertConfig(entity);
        if (result) {
            syncJobManager.ifAvailable(manager -> manager.applySchedule(entity));
        }
        return result ? RespInfo.success() : RespInfo.error("新增连接失败");
    }

    /**
     * 修改连接（不接收凭据字段）
     */
    @PutMapping
    @SaCheckPermission("system:collaboration:connection:update")
    @OperationLog(module = "企业协同连接", type = OperationType.UPDATE, desc = "修改连接")
    public RespInfo<Void> update(@Valid @RequestBody CollaborationConnectionSaveRequest request) {
        requireConnection(request.getId());
        SysSocialConfig entity = request.toEntity();
        boolean result = socialConfigService.updateConfig(entity);
        if (result) {
            syncJobManager.ifAvailable(manager -> manager.applySchedule(entity));
        }
        return result ? RespInfo.success() : RespInfo.error("修改连接失败");
    }

    /**
     * 删除连接
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("system:collaboration:connection:delete")
    @OperationLog(module = "企业协同连接", type = OperationType.DELETE, desc = "删除连接")
    public RespInfo<Void> remove(@PathVariable Long id) {
        requireConnection(id);
        boolean result = socialConfigService.deleteConfigById(id);
        if (result) {
            syncJobManager.ifAvailable(manager -> manager.removeSchedule(id));
        }
        return result ? RespInfo.success() : RespInfo.error("删除连接失败");
    }

    /**
     * 查询连接下物理应用（凭据脱敏）
     */
    @GetMapping("/{id}/apps")
    @SaCheckPermission("system:collaboration:connection:list")
    @OperationLog(module = "企业协同连接", type = OperationType.QUERY, desc = "查询连接应用列表")
    public RespInfo<List<CollaborationConnectionVO.AppVO>> listApps(@PathVariable Long id) {
        requireConnection(id);
        Long tenantId = CollaborationTenantHelper.currentTenantId();
        List<CollaborationConnectionVO.AppVO> apps = appConfigService.listApps(tenantId, id).stream()
                .map(app -> CollaborationConnectionVO.AppVO.from(app, appConfigService.secretSummary(app)))
                .toList();
        return RespInfo.success(apps);
    }

    /**
     * 新建物理应用（Secret 由服务层加密落库，操作日志不记录请求体）
     */
    @PostMapping("/{id}/apps")
    @SaCheckPermission("system:collaboration:connection:update")
    @OperationLog(module = "企业协同连接", type = OperationType.ADD, desc = "新建连接应用", saveRequestParams = false)
    public RespInfo<Void> createApp(@PathVariable Long id, @RequestBody CollaborationAppSaveRequest request) {
        requireConnection(id);
        boolean result = appConfigService.createApp(request.toCommand(CollaborationTenantHelper.currentTenantId(), id));
        return result ? RespInfo.success() : RespInfo.error("新建应用失败");
    }

    /**
     * 更新物理应用；Secret 空值/掩码回传零写保留，轮换支持 CAS 并发控制
     */
    @PutMapping("/{id}/apps")
    @SaCheckPermission("system:collaboration:connection:update")
    @OperationLog(module = "企业协同连接", type = OperationType.UPDATE, desc = "更新连接应用", saveRequestParams = false)
    public RespInfo<Void> updateApp(@PathVariable Long id, @RequestBody CollaborationAppSaveRequest request) {
        requireConnection(id);
        boolean result = appConfigService.updateApp(
                request.toCommand(CollaborationTenantHelper.currentTenantId(), id), request.getExpectedCredentialCipher());
        return result ? RespInfo.success() : RespInfo.error("更新应用失败");
    }

    /**
     * 删除物理应用（服务层校验未被启用中的能力绑定引用）
     */
    @DeleteMapping("/{id}/apps/{appId}")
    @SaCheckPermission("system:collaboration:connection:update")
    @OperationLog(module = "企业协同连接", type = OperationType.DELETE, desc = "删除连接应用")
    public RespInfo<Void> deleteApp(@PathVariable Long id, @PathVariable Long appId) {
        requireConnection(id);
        boolean result = appConfigService.deleteApp(CollaborationTenantHelper.currentTenantId(), appId);
        return result ? RespInfo.success() : RespInfo.error("删除应用失败");
    }

    /**
     * 绑定能力到物理应用（每连接每能力最多一个启用绑定）
     */
    @PostMapping("/{id}/bindings")
    @SaCheckPermission("system:collaboration:connection:update")
    @OperationLog(module = "企业协同连接", type = OperationType.UPDATE, desc = "绑定连接能力")
    public RespInfo<Void> bindCapability(@PathVariable Long id, @RequestBody CapabilityBindRequest request) {
        requireConnection(id);
        CollaborationCapability capability = parseCapability(request.getCapability());
        boolean result = appConfigService.bindCapability(CollaborationTenantHelper.currentTenantId(), id,
                capability, request.getAppConfigId());
        return result ? RespInfo.success() : RespInfo.error("绑定能力失败");
    }

    /**
     * 解绑连接能力
     */
    @DeleteMapping("/{id}/bindings/{capability}")
    @SaCheckPermission("system:collaboration:connection:update")
    @OperationLog(module = "企业协同连接", type = OperationType.UPDATE, desc = "解绑连接能力")
    public RespInfo<Void> unbindCapability(@PathVariable Long id, @PathVariable String capability) {
        requireConnection(id);
        boolean result = appConfigService.unbindCapability(CollaborationTenantHelper.currentTenantId(), id,
                parseCapability(capability));
        return result ? RespInfo.success() : RespInfo.error("解绑能力失败");
    }

    /**
     * 连通性测试：按能力绑定的应用获取 AccessToken，成功即通过；不回显任何 Token
     */
    @PostMapping("/{id}/test")
    @SaCheckPermission("system:collaboration:connection:test")
    @OperationLog(module = "企业协同连接", type = OperationType.OTHER, desc = "连接连通性测试")
    public RespInfo<String> test(@PathVariable Long id,
                                 @RequestParam(defaultValue = "MESSAGE") String capability) {
        SysSocialConfig connection = requireConnection(id);
        CollaborationCapability targetCapability;
        try {
            targetCapability = CollaborationCapability.valueOf(capability.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return RespInfo.error("不支持的能力类型: " + capability);
        }
        AccessTokenProvider provider = accessTokenProviders.stream()
                .filter(p -> p.platform().equals(connection.getPlatform()))
                .findFirst().orElse(null);
        if (provider == null) {
            return RespInfo.error("平台 " + connection.getPlatform() + " 暂未提供连通测试能力");
        }
        Long tenantId = CollaborationTenantHelper.currentTenantId();
        SysSocialAppConfig app = appConfigService.requireEnabledApp(tenantId, id, targetCapability);
        CollaborationExecutionContext context = new CollaborationExecutionContext(tenantId, id,
                connection.getConnectionCode(), connection.getPlatform(), connection.getEnterpriseId(),
                app.getId(), app.getAppCode(), app.getAgentId(), Map.of());
        // 目录能力使用通讯录专用 Token，其余能力使用应用级 Token
        AccessTokenProvider.TokenType tokenType = targetCapability == CollaborationCapability.DIRECTORY
                ? AccessTokenProvider.TokenType.CONTACT : AccessTokenProvider.TokenType.APP;
        provider.getAccessToken(context, tokenType);
        return RespInfo.success("连接凭据校验通过，Token 获取成功");
    }

    /**
     * 手工触发目录同步（编排层内部有分布式锁防并发）
     */
    @PostMapping("/{id}/sync")
    @SaCheckPermission("system:collaboration:sync:execute")
    @OperationLog(module = "企业协同连接", type = OperationType.OTHER, desc = "手工触发目录同步")
    public RespInfo<DirectorySyncResult> sync(@PathVariable Long id,
                                              @RequestBody(required = false) CollaborationSyncCommand command) {
        requireConnection(id);
        CollaborationSyncCommand syncCommand = command == null ? new CollaborationSyncCommand() : command;
        DirectorySyncResult result = syncOrchestrator.synchronize(id, syncCommand.toCommand(SessionHelper.getUserId()));
        return RespInfo.success(result);
    }

    /**
     * 加载连接并显式校验租户归属；超级管理员可跨租户管理连接
     */
    private SysSocialConfig requireConnection(Long id) {
        SysSocialConfig connection = id == null ? null : socialConfigService.selectConfigById(id);
        if (connection == null
                || (!SessionHelper.isAdmin()
                    && !CollaborationTenantHelper.currentTenantId().equals(connection.getTenantId()))) {
            throw new com.mdframe.forge.starter.core.exception.BusinessException("企业协同连接不存在或不属于当前租户");
        }
        return connection;
    }

    /**
     * 解析能力枚举，非法值统一转业务异常
     */
    private CollaborationCapability parseCapability(String capability) {
        try {
            return CollaborationCapability.valueOf(capability.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Exception e) {
            throw new com.mdframe.forge.starter.core.exception.BusinessException("不支持的能力类型: " + capability);
        }
    }

    /**
     * 能力绑定入参
     */
    @lombok.Data
    public static class CapabilityBindRequest {

        /** 业务能力：LOGIN/DIRECTORY/MESSAGE */
        private String capability;

        /** 目标物理应用ID */
        private Long appConfigId;
    }
}
