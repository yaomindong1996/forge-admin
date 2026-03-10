package com.mdframe.forge.starter.flow.controller;

import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import com.mdframe.forge.starter.flow.template.FlowTemplate;
import com.mdframe.forge.starter.flow.template.FlowTemplateFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程模板接口
 */
@RestController
@RequestMapping("/api/flow/template")
@RequiredArgsConstructor
public class FlowTemplateController {

    private final FlowTemplateFactory flowTemplateFactory;
    private final FlowModelService flowModelService;

    /**
     * 获取所有内置模板
     */
    @GetMapping("/list")
    public Map<String, Object> list() {
        List<FlowTemplate> templates = flowTemplateFactory.getAllTemplates();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", templates);
        return response;
    }

    /**
     * 从模板创建流程模型
     */
    @PostMapping("/create/{templateKey}")
    public Map<String, Object> createFromTemplate(
            @PathVariable String templateKey,
            @RequestBody Map<String, Object> params) {
        
        // 1. 获取模板
        FlowTemplate template = null;
        for (FlowTemplate t : flowTemplateFactory.getAllTemplates()) {
            if (t.getTemplateKey().equals(templateKey)) {
                template = t;
                break;
            }
        }
        
        if (template == null) {
            throw new RuntimeException("模板不存在：" + templateKey);
        }
        
        // 2. 创建流程模型
        FlowModel flowModel = new FlowModel();
        flowModel.setModelKey(templateKey);
        flowModel.setModelName(template.getTemplateName());
        flowModel.setCategory(template.getCategory());
        flowModel.setDescription(template.getDescription());
        flowModel.setFlowType(template.getCategory());
        flowModel.setFormType(template.getFormType());
        flowModel.setFormJson(template.getVariables());
        flowModel.setVersion(1);
        flowModel.setStatus(0);  // 设计态
        
        if (params.containsKey("createBy")) {
            flowModel.setCreateBy((String) params.get("createBy"));
        }
        
        FlowModel result = flowModelService.createModel(flowModel);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "从模板创建成功");
        response.put("data", result);
        return response;
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/{templateKey}")
    public Map<String, Object> getByKey(@PathVariable String templateKey) {
        FlowTemplate template = null;
        for (FlowTemplate t : flowTemplateFactory.getAllTemplates()) {
            if (t.getTemplateKey().equals(templateKey)) {
                template = t;
                break;
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", template);
        return response;
    }
}