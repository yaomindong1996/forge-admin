package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.mapper.FlowModelMapper;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.image.ProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 流程模型服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowModelServiceImpl extends ServiceImpl<FlowModelMapper, FlowModel> implements FlowModelService {

    @Autowired(required = false)
    private RepositoryService repositoryService;

    @Override
    public IPage<FlowModel> pageFlowModel(Page<FlowModel> page, String modelName, String category, Integer status) {
        LambdaQueryWrapper<FlowModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(modelName != null, FlowModel::getModelName, modelName)
                .eq(category != null, FlowModel::getCategory, category)
                .eq(status != null, FlowModel::getStatus, status)
                .orderByDesc(FlowModel::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowModel createModel(FlowModel flowModel) {
        // 生成唯一 KEY
        if (flowModel.getModelKey() == null || flowModel.getModelKey().isEmpty()) {
            flowModel.setModelKey("model_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        }
        
        // 初始状态为设计态
        flowModel.setStatus(0);
        flowModel.setVersion(1);
        flowModel.setDelFlag(0);
        flowModel.setCreateTime(LocalDateTime.now());
        flowModel.setUpdateTime(LocalDateTime.now());
        
        save(flowModel);
        log.info("创建流程模型成功：{}", flowModel.getModelKey());
        return flowModel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowModel updateModel(FlowModel flowModel) {
        flowModel.setUpdateTime(LocalDateTime.now());
        updateById(flowModel);
        log.info("更新流程模型成功：{}", flowModel.getModelKey());
        return flowModel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(String id) {
        FlowModel model = getById(id);
        if (model != null) {
            // 如果已部署，删除部署
            if (model.getDeploymentId() != null && !model.getDeploymentId().isEmpty()) {
                try {
                    repositoryService.deleteDeployment(model.getDeploymentId(), true);
                } catch (Exception e) {
                    log.warn("删除部署失败：{}", e.getMessage());
                }
            }
            removeById(id);
            log.info("删除流程模型成功：{}", model.getModelKey());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deployModel(String id) {
        FlowModel model = getById(id);
        if (model == null) {
            throw new RuntimeException("流程模型不存在");
        }
        
        if (model.getStatus() == 1) {
            throw new RuntimeException("流程模型已发布，请勿重复发布");
        }

        try {
            // TODO: 使用模型 BPMN XML 部署
            // 这里简化处理，实际需要读取模型的 XML 内容
            String deploymentKey = model.getModelKey() + "_v" + model.getVersion();
            
            Deployment deployment = repositoryService.createDeployment()
                    .name(model.getModelName())
                    .key(deploymentKey)
                    .addString(model.getModelKey() + ".bpmn20.xml", generateDefaultBpmnXml(model))
                    .deploy();
            
            // 更新模型状态
            model.setDeploymentId(deployment.getId());
            model.setDeploymentKey(deploymentKey);
            model.setStatus(1);
            model.setDeployTime(LocalDateTime.now());
            model.setVersion(model.getVersion() + 1);
            updateById(model);
            
            log.info("部署流程模型成功：{}，部署ID：{}", model.getModelKey(), deployment.getId());
            return deployment.getId();
            
        } catch (Exception e) {
            log.error("部署流程模型失败", e);
            throw new RuntimeException("部署失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableModel(String id) {
        FlowModel model = getById(id);
        if (model != null) {
            model.setStatus(2);
            updateById(model);
            log.info("禁用流程模型：{}", model.getModelKey());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableModel(String id) {
        FlowModel model = getById(id);
        if (model != null) {
            model.setStatus(1);
            updateById(model);
            log.info("启用流程模型：{}", model.getModelKey());
        }
    }

    @Override
    public FlowModel getModelDetail(String id) {
        return getById(id);
    }

    /**
     * 生成默认 BPMN XML（简化版）
     */
    private String generateDefaultBpmnXml(FlowModel model) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns:flowable=\"http://flowable.org/bpmn\" " +
                "xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" " +
                "xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" " +
                "xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" " +
                "typeLanguage=\"http://www.w3.org/2001/XMLSchema\" " +
                "expressionLanguage=\"http://www.w3.org/1999/XPath\" " +
                "targetNamespace=\"http://www.flowable.org/processdef\">\n" +
                "  <process id=\"" + model.getModelKey() + "\" name=\"" + model.getModelName() + "\" isExecutable=\"true\">\n" +
                "    <startEvent id=\"startEvent\" name=\"开始\"/>\n" +
                "    <userTask id=\"approveTask\" name=\"审批\" flowable:assignee=\"${initiator}\"/>\n" +
                "    <endEvent id=\"endEvent\" name=\"结束\"/>\n" +
                "    <sequenceFlow id=\"flow1\" sourceRef=\"startEvent\" targetRef=\"approveTask\"/>\n" +
                "    <sequenceFlow id=\"flow2\" sourceRef=\"approveTask\" targetRef=\"endEvent\"/>\n" +
                "  </process>\n" +
                "</definitions>";
    }
}