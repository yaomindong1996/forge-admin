package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessTrigger;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessTriggerLog;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessPermissionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessTriggerService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTriggerScenarioTemplateVO;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 业务触发器管理控制器。
 */
@RestController
@RequestMapping("/ai/business/trigger")
@RequiredArgsConstructor
public class BusinessTriggerController {

    private final BusinessTriggerService triggerService;
    private final BusinessFlowService flowService;
    private final BusinessPermissionService permissionService;

    /**
     * 分页查询触发器
     */
    @GetMapping("/page")
    @SaCheckPermission("ai:businessTrigger:list")
    public RespInfo<Page<AiBusinessTrigger>> page(@RequestParam(required = false) String objectCode,
                                                   @RequestParam(required = false) String scenarioType,
                                                   PageQuery pageQuery) {
        return RespInfo.success(triggerService.selectPage(objectCode, scenarioType, pageQuery));
    }

    /**
     * 查询触发器详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("ai:businessTrigger:list")
    public RespInfo<AiBusinessTrigger> getById(@PathVariable Long id) {
        return RespInfo.success(triggerService.selectById(id));
    }

    /**
     * 新增触发器
     */
    @PostMapping
    @SaCheckPermission("ai:businessTrigger:add")
    public RespInfo<Void> create(@RequestBody AiBusinessTrigger trigger) {
        triggerService.insert(trigger);
        return RespInfo.success();
    }

    /**
     * 修改触发器
     */
    @PutMapping
    @SaCheckPermission("ai:businessTrigger:edit")
    public RespInfo<Void> update(@RequestBody AiBusinessTrigger trigger) {
        triggerService.update(trigger);
        return RespInfo.success();
    }

    /**
     * 删除触发器
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("ai:businessTrigger:delete")
    public RespInfo<Void> delete(@PathVariable Long id) {
        triggerService.deleteById(id);
        return RespInfo.success();
    }

    /**
     * 启停触发器
     */
    @PutMapping("/{id}/status/{status}")
    @SaCheckPermission("ai:businessTrigger:edit")
    public RespInfo<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        triggerService.updateStatus(id, status);
        return RespInfo.success();
    }

    /**
     * 查询触发器执行日志
     */
    @GetMapping("/logs")
    @SaCheckPermission("ai:businessTrigger:list")
    public RespInfo<Page<AiBusinessTriggerLog>> logs(@RequestParam(required = false) Long triggerId,
                                                      PageQuery pageQuery) {
        return RespInfo.success(triggerService.selectLogPage(triggerId, pageQuery));
    }

    /**
     * 查询业务触发器场景模板。
     */
    @GetMapping("/scenario-templates")
    @SaCheckPermission("ai:businessTrigger:list")
    public RespInfo<List<BusinessTriggerScenarioTemplateVO>> scenarioTemplates() {
        return RespInfo.success(triggerService.scenarioTemplates());
    }

    // ==================== 流程集成接口 ====================

    /**
     * 手动发起业务流程
     */
    @PostMapping("/flow/start")
    public RespInfo<JSONObject> startFlow(@RequestBody Map<String, Object> params) {
        String objectCode = (String) params.get("objectCode");
        String recordId = String.valueOf(params.get("recordId"));
        @SuppressWarnings("unchecked")
        Map<String, Object> recordData = (Map<String, Object>) params.get("recordData");
        return RespInfo.success(flowService.startFlow(objectCode, recordId, recordData));
    }

    /**
     * 查询业务对象的流程绑定配置
     */
    @GetMapping("/flow/binding/{objectCode}")
    @SaCheckPermission("ai:businessFlow:config")
    public RespInfo<JSONObject> getFlowBinding(@PathVariable String objectCode) {
        return RespInfo.success(flowService.getFlowBindingLegacy(objectCode));
    }

    /**
     * 保存流程绑定配置
     */
    @PostMapping("/flow/binding/{objectCode}")
    @SaCheckPermission("ai:businessFlow:config")
    public RespInfo<Void> saveFlowBinding(@PathVariable String objectCode,
                                           @RequestBody JSONObject config) {
        flowService.saveFlowBinding(objectCode, config);
        return RespInfo.success();
    }

    /**
     * 查询业务记录的流程状态
     */
    @GetMapping("/flow/status/{objectCode}/{recordId}")
    public RespInfo<JSONObject> getFlowStatus(@PathVariable String objectCode,
                                               @PathVariable String recordId) {
        String businessKey = objectCode + ":" + recordId;
        return RespInfo.success(flowService.getFlowStatus(businessKey));
    }

    // ==================== 权限集成接口 ====================

    /**
     * 查询业务对象的权限概览
     */
    @GetMapping("/permission/{objectCode}")
    public RespInfo<Map<String, Object>> getPermissionOverview(@PathVariable String objectCode) {
        return RespInfo.success(permissionService.getPermissionOverview(objectCode));
    }
}
