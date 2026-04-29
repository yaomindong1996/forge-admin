package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 流程模型管理接口
 */
@RestController
@RequestMapping("/api/flow/model")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
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
     * 获取启用的模型列表
     */
    @GetMapping("/enabled")
    public RespInfo<List<FlowModel>> getEnabledModels(
            @RequestParam(required = false) String category) {
        List<FlowModel> models = flowModelService.getEnabledModels(category);
        return RespInfo.success(models);
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
     * 根据Key获取模型
     */
    @GetMapping("/key/{modelKey}")
    public RespInfo<FlowModel> getByKey(@PathVariable String modelKey) {
        FlowModel model = flowModelService.getModelByKey(modelKey);
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
     * 挂起流程模型
     */
    @PostMapping("/{id}/suspend")
    public RespInfo<Void> suspend(@PathVariable String id) {
        flowModelService.suspendModel(id);
        return RespInfo.success("挂起成功", null);
    }

    /**
     * 激活流程模型
     */
    @PostMapping("/{id}/activate")
    public RespInfo<Void> activate(@PathVariable String id) {
        flowModelService.activateModel(id);
        return RespInfo.success("激活成功", null);
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
     * 获取模型版本历史
     */
    @GetMapping("/{modelKey}/versions")
    public RespInfo<List<Map<String, Object>>> getVersions(@PathVariable String modelKey) {
        List<Map<String, Object>> versions = flowModelService.getModelVersions(modelKey);
        return RespInfo.success(versions);
    }

    /**
     * 导入BPMN模型
     */
    @PostMapping("/import")
    public RespInfo<FlowModel> importModel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String category) {
        try {
            String bpmnXml = new String(file.getBytes(), StandardCharsets.UTF_8);
            FlowModel model = flowModelService.importModel(bpmnXml, modelName, category);
            return RespInfo.success("导入成功", model);
        } catch (Exception e) {
            return RespInfo.error("导入失败：" + e.getMessage());
        }
    }

    /**
     * 导出BPMN XML
     */
    @GetMapping("/{id}/export")
    public RespInfo<String> exportModel(@PathVariable String id) {
        String bpmnXml = flowModelService.exportModel(id);
        return RespInfo.success(bpmnXml);
    }

    /**
     * 复制模型
     */
    @PostMapping("/{id}/copy")
    public RespInfo<FlowModel> copy(
            @PathVariable String id,
            @RequestParam String newName) {
        FlowModel model = flowModelService.copyModel(id, newName);
        return RespInfo.success("复制成功", model);
    }

    /**
     * 检查模型Key是否存在
     */
    @GetMapping("/checkKey")
    public RespInfo<Boolean> checkKeyExists(
            @RequestParam String modelKey,
            @RequestParam(required = false) String excludeId) {
        boolean exists = flowModelService.checkModelKeyExists(modelKey, excludeId);
        return RespInfo.success(exists);
    }

    /**
     * 获取流程模型列表（下拉选择用）
     */
    @GetMapping("/list")
    public RespInfo<List<FlowModel>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        List<FlowModel> models = flowModelService.getEnabledModels(category);
        return RespInfo.success(models);
    }
}
