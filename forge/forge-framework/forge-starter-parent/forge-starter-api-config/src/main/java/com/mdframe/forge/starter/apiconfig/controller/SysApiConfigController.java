package com.mdframe.forge.starter.apiconfig.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigQuery;
import com.mdframe.forge.starter.apiconfig.domain.entity.SysApiConfig;
import com.mdframe.forge.starter.apiconfig.domain.event.ApiConfigRefreshEvent;
import com.mdframe.forge.starter.apiconfig.service.ISysApiConfigService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API配置管理Controller
 */
@RestController
@RequestMapping("/system/apiConfig")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class SysApiConfigController {

    private final ISysApiConfigService apiConfigService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 分页查询API配置列表
     */
    @GetMapping("/page")
    @OperationLog(module = "API配置管理", type = OperationType.QUERY, desc = "分页查询配置列表")
    public RespInfo<Page<SysApiConfig>> page(PageQuery pageQuery, ApiConfigQuery query) {
        Page<SysApiConfig> page = apiConfigService.selectConfigPage(pageQuery.toPage(), query);
        return RespInfo.success(page);
    }

    /**
     * 查询API配置列表
     */
    @GetMapping("/list")
    @OperationLog(module = "API配置管理", type = OperationType.QUERY, desc = "查询配置列表")
    public RespInfo<List<SysApiConfig>> list(ApiConfigQuery query) {
        List<SysApiConfig> list = apiConfigService.selectConfigList(query);
        return RespInfo.success(list);
    }

    /**
     * 根据ID查询配置详情
     */
    @PostMapping("/getById")
    public RespInfo<SysApiConfig> getById(@RequestParam Long id) {
        SysApiConfig config = apiConfigService.selectConfigById(id);
        return RespInfo.success(config);
    }
    
    @GetMapping("/getById")
    public RespInfo<SysApiConfig> getByIdGet(@RequestParam Long id) {
        SysApiConfig config = apiConfigService.selectConfigById(id);
        return RespInfo.success(config);
    }

    /**
     * 新增API配置
     */
    @PostMapping("/add")
    @OperationLog(module = "API配置管理", type = OperationType.ADD, desc = "新增API配置")
    public RespInfo<Void> add(@RequestBody SysApiConfig config) {
        boolean result = apiConfigService.insertConfig(config);
        if (result) {
            // 发布刷新事件
            eventPublisher.publishEvent(new ApiConfigRefreshEvent(this,
                    ApiConfigRefreshEvent.RefreshType.SINGLE,
                    config.getId(),
                    "新增API配置"));
        }
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改API配置
     */
    @PostMapping("/edit")
    @OperationLog(module = "API配置管理", type = OperationType.UPDATE, desc = "修改API配置")
    public RespInfo<Void> edit(@RequestBody SysApiConfig config) {
        boolean result = apiConfigService.updateConfig(config);
        if (result) {
            // 发布刷新事件
            eventPublisher.publishEvent(new ApiConfigRefreshEvent(this,
                    ApiConfigRefreshEvent.RefreshType.SINGLE,
                    config.getId(),
                    "修改API配置"));
        }
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除API配置
     */
    @PostMapping("/remove")
    @OperationLog(module = "API配置管理", type = OperationType.DELETE, desc = "删除API配置")
    public RespInfo<Void> remove(@RequestParam Long id) {
        boolean result = apiConfigService.deleteConfigById(id);
        if (result) {
            // 发布刷新事件
            eventPublisher.publishEvent(new ApiConfigRefreshEvent(this,
                    ApiConfigRefreshEvent.RefreshType.SINGLE,
                    id,
                    "删除API配置"));
        }
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    /**
     * 批量删除API配置
     */
    @PostMapping("/removeBatch")
    @OperationLog(module = "API配置管理", type = OperationType.DELETE, desc = "批量删除API配置")
    public RespInfo<Void> removeBatch(@RequestBody Long[] ids) {
        boolean result = apiConfigService.deleteConfigByIds(ids);
        if (result) {
            // 发布刷新事件
            eventPublisher.publishEvent(new ApiConfigRefreshEvent(this,
                    ApiConfigRefreshEvent.RefreshType.ALL));
        }
        return result ? RespInfo.success() : RespInfo.error("批量删除失败");
    }
}
