package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 流程模型管理接口
 */
@RestController
@RequestMapping("/api/flow/model")
@RequiredArgsConstructor
public class FlowModelController {

    private final FlowModelService flowModelService;

    /**
     * 分页查询流程模型
     */
    @GetMapping("/page")
    public RespInfo<IPage<FlowModel>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        
        Page<FlowModel> page = new Page<>(pageNum, pageSize);
        IPage<FlowModel> result = flowModelService.pageFlowModel(page, modelName, category, status);
        return RespInfo.success(result);
    }

    /**
     * 获取模型详情
     */
    @GetMapping("/{id}")
    public RespInfo<FlowModel> getById(@PathVariable String id) {
        FlowModel model = flowModelService.getModelDetail(id);
        return RespInfo.success(model);
    }

    /**
     * 创建流程模型
     */
    @PostMapping
    public RespInfo<FlowModel> create(@RequestBody FlowModel flowModel) {
        FlowModel result = flowModelService.createModel(flowModel);
        return RespInfo.success("创建成功", result);
    }

    /**
     * 更新流程模型
     */
    @PutMapping
    public RespInfo<FlowModel> update(@RequestBody FlowModel flowModel) {
        FlowModel result = flowModelService.updateModel(flowModel);
        return RespInfo.success("更新成功", result);
    }

    /**
     * 删除流程模型
     */
    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable String id) {
        flowModelService.deleteModel(id);
        return RespInfo.success("删除成功", null);
    }

    /**
     * 部署流程模型
     */
    @PostMapping("/{id}/deploy")
    public RespInfo<String> deploy(@PathVariable String id) {
        String deploymentId = flowModelService.deployModel(id);
        return RespInfo.success("部署成功", deploymentId);
    }

    /**
     * 禁用流程模型
     */
    @PostMapping("/{id}/disable")
    public RespInfo<Void> disable(@PathVariable String id) {
        flowModelService.disableModel(id);
        return RespInfo.success("禁用成功", null);
    }

    /**
     * 启用流程模型
     */
    @PostMapping("/{id}/enable")
    public RespInfo<Void> enable(@PathVariable String id) {
        flowModelService.enableModel(id);
        return RespInfo.success("启用成功", null);
    }

    /**
     * 获取流程模型列表（下拉选择用）
     */
    @GetMapping("/list")
    public RespInfo<IPage<FlowModel>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        
        Page<FlowModel> page = new Page<>(1, 100);
        IPage<FlowModel> result = flowModelService.pageFlowModel(page, null, category, status);
        return RespInfo.success(result);
    }
}