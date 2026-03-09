package com.mdframe.forge.starter.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
    public Map<String, Object> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        
        Page<FlowModel> page = new Page<>(pageNum, pageSize);
        IPage<FlowModel> result = flowModelService.pageFlowModel(page, modelName, category, status);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        return response;
    }

    /**
     * 获取模型详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable String id) {
        FlowModel model = flowModelService.getModelDetail(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", model);
        return response;
    }

    /**
     * 创建流程模型
     */
    @PostMapping
    public Map<String, Object> create(@RequestBody FlowModel flowModel) {
        FlowModel result = flowModelService.createModel(flowModel);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "创建成功");
        response.put("data", result);
        return response;
    }

    /**
     * 更新流程模型
     */
    @PutMapping
    public Map<String, Object> update(@RequestBody FlowModel flowModel) {
        FlowModel result = flowModelService.updateModel(flowModel);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "更新成功");
        response.put("data", result);
        return response;
    }

    /**
     * 删除流程模型
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        flowModelService.deleteModel(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "删除成功");
        return response;
    }

    /**
     * 部署流程模型
     */
    @PostMapping("/{id}/deploy")
    public Map<String, Object> deploy(@PathVariable String id) {
        String deploymentId = flowModelService.deployModel(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "部署成功");
        response.put("deploymentId", deploymentId);
        return response;
    }

    /**
     * 禁用流程模型
     */
    @PostMapping("/{id}/disable")
    public Map<String, Object> disable(@PathVariable String id) {
        flowModelService.disableModel(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "禁用成功");
        return response;
    }

    /**
     * 启用流程模型
     */
    @PostMapping("/{id}/enable")
    public Map<String, Object> enable(@PathVariable String id) {
        flowModelService.enableModel(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "启用成功");
        return response;
    }

    /**
     * 获取流程模型列表（下拉选择用）
     */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        
        Page<FlowModel> page = new Page<>(1, 100);
        IPage<FlowModel> result = flowModelService.pageFlowModel(page, null, category, status);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result.getRecords());
        return response;
    }
}