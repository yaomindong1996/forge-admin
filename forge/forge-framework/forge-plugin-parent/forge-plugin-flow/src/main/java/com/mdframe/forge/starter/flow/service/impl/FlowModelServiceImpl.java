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
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.image.ProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        wrapper.like(modelName != null && !modelName.isEmpty(), FlowModel::getModelName, modelName)
                .eq(category != null && !category.isEmpty(), FlowModel::getCategory, category)
                .eq(status != null, FlowModel::getStatus, status)
                .orderByDesc(FlowModel::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<FlowModel> getEnabledModels(String category) {
        LambdaQueryWrapper<FlowModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowModel::getStatus, 1)
                .eq(category != null && !category.isEmpty(), FlowModel::getCategory, category)
                .orderByDesc(FlowModel::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowModel createModel(FlowModel flowModel) {
        // 生成唯一 KEY
        if (flowModel.getModelKey() == null || flowModel.getModelKey().isEmpty()) {
            flowModel.setModelKey("model_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        }
        
        // 检查Key是否重复
        if (checkModelKeyExists(flowModel.getModelKey(), null)) {
            throw new RuntimeException("模型Key已存在：" + flowModel.getModelKey());
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
        FlowModel existing = getById(flowModel.getId());
        if (existing == null) {
            throw new RuntimeException("流程模型不存在");
        }
        
        // 已发布的模型不允许修改Key
        if (existing.getStatus() == 1 && !existing.getModelKey().equals(flowModel.getModelKey())) {
            throw new RuntimeException("已发布的模型不允许修改Key");
        }
        
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
            if (model.getDeploymentId() != null && !model.getDeploymentId().isEmpty() && repositoryService != null) {
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
        
        if (model.getBpmnXml() == null || model.getBpmnXml().isEmpty()) {
            throw new RuntimeException("请先设计流程图");
        }

        try {
            if (repositoryService == null) {
                throw new RuntimeException("Flowable未初始化");
            }
            
            String deploymentKey = model.getModelKey() + "_v" + model.getVersion();
            
            Deployment deployment = repositoryService.createDeployment()
                    .name(model.getModelName())
                    .key(deploymentKey)
                    .addString(model.getModelKey() + ".bpmn20.xml", model.getBpmnXml())
                    .deploy();
            
            // 获取流程定义
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();
            
            // 更新模型状态
            model.setDeploymentId(deployment.getId());
            model.setDeploymentKey(deploymentKey);
            model.setProcessDefinitionId(processDefinition != null ? processDefinition.getId() : null);
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
    public void suspendModel(String id) {
        FlowModel model = getById(id);
        if (model == null) {
            throw new RuntimeException("流程模型不存在");
        }
        
        if (model.getStatus() != 1) {
            throw new RuntimeException("只有已发布的模型才能挂起");
        }
        
        // 挂起Flowable流程定义
        if (model.getProcessDefinitionId() != null && repositoryService != null) {
            repositoryService.suspendProcessDefinitionById(model.getProcessDefinitionId());
        }
        
        model.setStatus(2); // 已挂起
        updateById(model);
        log.info("挂起流程模型：{}", model.getModelKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateModel(String id) {
        FlowModel model = getById(id);
        if (model == null) {
            throw new RuntimeException("流程模型不存在");
        }
        
        if (model.getStatus() != 2) {
            throw new RuntimeException("只有已挂起的模型才能激活");
        }
        
        // 激活Flowable流程定义
        if (model.getProcessDefinitionId() != null && repositoryService != null) {
            repositoryService.activateProcessDefinitionById(model.getProcessDefinitionId());
        }
        
        model.setStatus(1); // 已发布
        updateById(model);
        log.info("激活流程模型：{}", model.getModelKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableModel(String id) {
        FlowModel model = getById(id);
        if (model != null) {
            model.setStatus(3); // 已禁用
            updateById(model);
            log.info("禁用流程模型：{}", model.getModelKey());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableModel(String id) {
        FlowModel model = getById(id);
        if (model != null) {
            model.setStatus(0); // 设计态
            updateById(model);
            log.info("启用流程模型：{}", model.getModelKey());
        }
    }

    @Override
    public FlowModel getModelDetail(String id) {
        return getById(id);
    }

    @Override
    public FlowModel getModelByKey(String modelKey) {
        LambdaQueryWrapper<FlowModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowModel::getModelKey, modelKey);
        return getOne(wrapper);
    }

    @Override
    public List<Map<String, Object>> getModelVersions(String modelKey) {
        // 查询Flowable中的历史版本
        if (repositoryService == null) {
            return Collections.emptyList();
        }
        
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(modelKey)
                .orderByProcessDefinitionVersion()
                .desc()
                .list();
        
        return definitions.stream().map(pd -> {
            Map<String, Object> version = new HashMap<>();
            version.put("id", pd.getId());
            version.put("key", pd.getKey());
            version.put("name", pd.getName());
            version.put("version", pd.getVersion());
            version.put("deploymentId", pd.getDeploymentId());
            version.put("suspended", pd.isSuspended());
            // 从Deployment获取部署时间
            try {
                if (pd.getDeploymentId() != null) {
                    var deployment = repositoryService.createDeploymentQuery()
                            .deploymentId(pd.getDeploymentId())
                            .singleResult();
                    if (deployment != null) {
                        version.put("deploymentTime", deployment.getDeploymentTime());
                    }
                }
            } catch (Exception e) {
                log.warn("获取部署时间失败", e);
            }
            return version;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowModel importModel(String bpmnXml, String modelName, String category) {
        if (bpmnXml == null || bpmnXml.isEmpty()) {
            throw new RuntimeException("BPMN XML不能为空");
        }
        
        // 从XML中提取流程Key
        String modelKey = extractProcessKey(bpmnXml);
        if (modelKey == null) {
            modelKey = "imported_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        
        // 检查Key是否重复
        if (checkModelKeyExists(modelKey, null)) {
            modelKey = modelKey + "_" + System.currentTimeMillis();
        }
        
        FlowModel model = new FlowModel();
        model.setModelKey(modelKey);
        model.setModelName(modelName != null ? modelName : "导入的流程");
        model.setCategory(category);
        model.setBpmnXml(bpmnXml);
        model.setStatus(0);
        model.setVersion(1);
        model.setDelFlag(0);
        model.setCreateTime(LocalDateTime.now());
        model.setUpdateTime(LocalDateTime.now());
        
        save(model);
        log.info("导入流程模型成功：{}", model.getModelKey());
        return model;
    }

    @Override
    public String exportModel(String id) {
        FlowModel model = getById(id);
        if (model == null) {
            throw new RuntimeException("流程模型不存在");
        }
        return model.getBpmnXml();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowModel copyModel(String id, String newName) {
        FlowModel source = getById(id);
        if (source == null) {
            throw new RuntimeException("源模型不存在");
        }
        
        String newKey = source.getModelKey() + "_copy_" + System.currentTimeMillis();
        
        FlowModel newModel = new FlowModel();
        newModel.setModelKey(newKey);
        newModel.setModelName(newName);
        newModel.setCategory(source.getCategory());
        newModel.setDescription(source.getDescription());
        newModel.setFlowType(source.getFlowType());
        newModel.setFormType(source.getFormType());
        newModel.setFormId(source.getFormId());
        newModel.setFormJson(source.getFormJson());
        newModel.setBpmnXml(source.getBpmnXml());
        newModel.setStatus(0);
        newModel.setVersion(1);
        newModel.setDelFlag(0);
        newModel.setCreateTime(LocalDateTime.now());
        newModel.setUpdateTime(LocalDateTime.now());
        
        save(newModel);
        log.info("复制流程模型成功，源模型：{}，新模型：{}", source.getModelKey(), newModel.getModelKey());
        return newModel;
    }

    @Override
    public boolean checkModelKeyExists(String modelKey, String excludeId) {
        LambdaQueryWrapper<FlowModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowModel::getModelKey, modelKey);
        if (excludeId != null && !excludeId.isEmpty()) {
            wrapper.ne(FlowModel::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    /**
     * 从BPMN XML中提取流程Key
     */
    private String extractProcessKey(String bpmnXml) {
        try {
            // 简单解析，提取process id
            int start = bpmnXml.indexOf("<bpmn:process id=\"");
            if (start == -1) {
                start = bpmnXml.indexOf("<process id=\"");
            }
            if (start == -1) {
                return null;
            }
            
            start = bpmnXml.indexOf("id=\"", start) + 4;
            int end = bpmnXml.indexOf("\"", start);
            return bpmnXml.substring(start, end);
        } catch (Exception e) {
            log.warn("提取流程Key失败", e);
            return null;
        }
    }
}