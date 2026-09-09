package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.dto.FlowStartConfig;
import com.mdframe.forge.starter.flow.dto.FlowModelSortDTO;
import com.mdframe.forge.starter.flow.dto.FlowModelSortItemDTO;
import com.mdframe.forge.starter.flow.vo.FlowModelStatisticsVO;
import com.mdframe.forge.starter.flow.vo.FlowModelVersionSummaryVO;
import com.mdframe.forge.starter.flow.enums.FlowModelStatus;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowModelMapper;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.flow.event.FlowModelPublishEvent;
import com.mdframe.forge.starter.flow.helper.BpmnXmlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.impl.util.io.BytesStreamSource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.image.ProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
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

    private static final String MODEL_KEY_PATTERN = "^[A-Za-z][A-Za-z0-9_-]{1,63}$";
    private static final int MAX_MODEL_VERSIONS = 100;

    @Autowired(required = false)
    private RepositoryService repositoryService;

    @Autowired(required = false)
    private RuntimeService runtimeService;

    @Autowired(required = false)
    private HistoryService historyService;

    @Autowired
    private FlowBusinessMapper flowBusinessMapper;
    
    @Autowired(required = false)
    private ProcessEngineConfiguration processEngineConfiguration;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 组织解析仅用于发布/发起前预检，不改变 BPMN 运行时的候选人计算。
     * 采用字段注入保持与可选流程插件装配方式兼容。
     */
    @Autowired(required = false)
    private FlowOrgIntegrationService flowOrgIntegrationService;

    @Override
    public IPage<FlowModel> pageFlowModel(Page<FlowModel> page, String modelName, String category, Integer status) {
        String currentUsername = SessionHelper.getUsername();
        Long tenantId = SessionHelper.getTenantId();
        if (currentUsername == null || currentUsername.isEmpty() || tenantId == null || tenantId <= 0) {
            return new Page<>(page.getCurrent(), page.getSize());
        }
        return this.getBaseMapper().selectModelPage(page, modelName, category, status, currentUsername, tenantId);
    }

    @Override
    public List<FlowModel> getEnabledModels(String category) {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            log.warn("查询流程模型目录时缺少可信租户上下文");
            return List.of();
        }
        return this.getBaseMapper().selectEnabledModels(tenantId, category);
    }

    @Override
    public FlowModelStatisticsVO getStatusStatistics(String modelName, String category) {
        String currentUsername = SessionHelper.getUsername();
        Long tenantId = SessionHelper.getTenantId();
        if (currentUsername == null || currentUsername.isEmpty() || tenantId == null || tenantId <= 0) {
            return emptyStatusStatistics();
        }

        Map<String, Object> raw = this.getBaseMapper().selectStatusStatistics(
                modelName, category, currentUsername, tenantId);
        FlowModelStatisticsVO result = new FlowModelStatisticsVO();
        result.setTotal(readCount(raw, "total"));
        result.setDesigning(readCount(raw, "designing"));
        result.setDeployed(readCount(raw, "deployed"));
        result.setSuspended(readCount(raw, "suspended"));
        result.setDisabled(readCount(raw, "disabled"));
        return result;
    }

    private FlowModelStatisticsVO emptyStatusStatistics() {
        FlowModelStatisticsVO result = new FlowModelStatisticsVO();
        result.setTotal(0L);
        result.setDesigning(0L);
        result.setDeployed(0L);
        result.setSuspended(0L);
        result.setDisabled(0L);
        return result;
    }

    private long readCount(Map<String, Object> raw, String key) {
        if (raw == null || raw.isEmpty()) {
            return 0L;
        }
        Object value = raw.get(key);
        if (value == null) {
            value = raw.get(key.toUpperCase(Locale.ROOT));
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowModel createModel(FlowModel flowModel) {
        Long tenantId = requireTenantId();
        flowModel.setTenantId(tenantId);
        flowModel.setDesignerType(normalizeDesignerType(flowModel.getDesignerType()));
        // 新建流程统一使用 Redis 事件通知；管理端不再暴露通知方式选择。
        flowModel.setNotifyType("redis");
        String requestedModelKey = flowModel.getModelKey() == null ? null : flowModel.getModelKey().trim();
        flowModel.setModelKey(requestedModelKey == null || requestedModelKey.isEmpty()
                ? generateModelKey() : validateModelKey(requestedModelKey));
        if (flowModel.getBpmnXml() != null && !flowModel.getBpmnXml().isBlank()) {
            flowModel.setBpmnXml(normalizeBpmnXml(flowModel.getBpmnXml(), "创建流程模型"));
        }
        
        // 检查Key是否重复
        if (checkModelKeyExists(flowModel.getModelKey(), null)) {
            throw new RuntimeException("模型Key已存在：" + flowModel.getModelKey());
        }
        
        // 初始状态为设计态
        flowModel.setStatus(FlowModelStatus.DESIGNING.getCode());
        flowModel.setVersion(1);
        flowModel.setDelFlag(0);
        flowModel.setCreateTime(LocalDateTime.now());
        flowModel.setUpdateTime(LocalDateTime.now());
        flowModel.setCreateBy(SessionHelper.getLoginUser().getUsername());
        try {
            save(flowModel);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("模型Key已存在：" + flowModel.getModelKey(), exception);
        }
        log.info("创建流程模型成功：{}", flowModel.getModelKey());
        return flowModel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowModel updateModel(FlowModel flowModel) {
        Long tenantId = requireTenantId();
        FlowModel existing = getModelByIdAndTenant(flowModel.getId(), tenantId);
        if (existing == null) {
            throw new RuntimeException("流程模型不存在");
        }
        if (flowModel.getTenantId() != null && !tenantId.equals(flowModel.getTenantId())) {
            throw new RuntimeException("无权修改其他租户流程模型");
        }
        flowModel.setTenantId(tenantId);
        if (flowModel.getDesignerType() == null || flowModel.getDesignerType().isBlank()) {
            flowModel.setDesignerType(normalizeDesignerType(existing.getDesignerType()));
        } else {
            flowModel.setDesignerType(normalizeDesignerType(flowModel.getDesignerType()));
        }

        String requestedModelKey = flowModel.getModelKey() == null ? "" : flowModel.getModelKey().trim();
        String nextModelKey = requestedModelKey.isEmpty()
                ? existing.getModelKey() : validateModelKey(requestedModelKey);
        if (!Objects.equals(existing.getModelKey(), nextModelKey)
                && checkModelKeyExists(nextModelKey, flowModel.getId())) {
            throw new IllegalArgumentException("模型Key已存在：" + nextModelKey);
        }

        // 已发布或挂起的模型已经被运行实例/部署引用，不允许修改 Key。
        if ((FlowModelStatus.PUBLISHED.matches(existing.getStatus())
                || FlowModelStatus.SUSPENDED.matches(existing.getStatus()))
                && !existing.getModelKey().equals(nextModelKey)) {
            throw new RuntimeException("已发布的模型不允许修改Key");
        }
        flowModel.setModelKey(nextModelKey);
        if (flowModel.getBpmnXml() != null && !flowModel.getBpmnXml().isBlank()) {
            flowModel.setBpmnXml(normalizeBpmnXml(flowModel.getBpmnXml(), "更新流程模型"));
        }
        flowModel.setLastUpdateBy(SessionHelper.getLoginUser().getUsername());
        flowModel.setUpdateTime(LocalDateTime.now());
        try {
            updateById(flowModel);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("模型Key已存在：" + nextModelKey, exception);
        }
        log.info("更新流程模型成功：{}", flowModel.getModelKey());
        return flowModel;
    }

    private String normalizeDesignerType(String designerType) {
        return "business".equals(designerType) ? "business" : "approval";
    }

    private String generateModelKey() {
        String modelKey;
        do {
            modelKey = "model_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        } while (checkModelKeyExists(modelKey, null));
        return modelKey;
    }

    private String validateModelKey(String modelKey) {
        if (!modelKey.matches(MODEL_KEY_PATTERN)) {
            throw new IllegalArgumentException("模型Key必须以字母开头，且只能包含字母、数字、下划线或短横线，长度为2-64位");
        }
        return modelKey;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(String id) {
        FlowModel model = getModelByIdAndTenant(id, requireTenantId());
        if (model != null) {
            validateNoProcessData(model);

            // 如果已部署，删除部署
            if (model.getDeploymentId() != null && !model.getDeploymentId().isEmpty() && repositoryService != null) {
                try {
                    repositoryService.deleteDeployment(model.getDeploymentId(), false);
                } catch (Exception e) {
                    log.warn("删除流程模型部署失败：modelKey={}, deploymentId={}",
                            model.getModelKey(), model.getDeploymentId(), e);
                    throw new BusinessException(500, "删除流程部署失败：" + e.getMessage());
                }
            }
            removeById(id);
            log.info("删除流程模型成功：{}", model.getModelKey());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deployModel(String id) {
        return deployModel(id, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deployModel(String id, String changeDescription) {
        FlowModel model = getModelByIdAndTenant(id, requireTenantId());
        if (model == null) {
            throw new RuntimeException("流程模型不存在");
        }
        
        if (model.getBpmnXml() == null || model.getBpmnXml().isEmpty()) {
            throw new RuntimeException("请先设计流程图");
        }
        
        // 检查 BPMN XML 是否包含图形信息
        String bpmnXml = model.getBpmnXml();
        if (!bpmnXml.contains("BPMNDiagram") && !bpmnXml.contains("bpmndi:BPMNDiagram")) {
            log.error("BPMN XML 缺少图形信息，无法部署。XML 长度: {}, 内容预览: {}",
                    bpmnXml.length(),
                    bpmnXml.length() > 500 ? bpmnXml.substring(0, 500) + "..." : bpmnXml);
            throw new RuntimeException("流程图数据不完整，缺少图形坐标信息。请在流程设计器中重新设计流程图并保存后再部署。");
        }
        
        // 将 BPMN XML 中的 process id 替换为 modelKey，确保启动流程时能找到正确的流程定义
        String modelKey = model.getModelKey();
        String originalProcessId = extractProcessKey(bpmnXml);
        bpmnXml = replaceProcessId(bpmnXml, modelKey);
        bpmnXml = normalizeBpmnXml(bpmnXml, "部署流程模型");
        if (Objects.equals(originalProcessId, modelKey)) {
            log.debug("流程ID已与模型Key一致：{}", modelKey);
        } else {
            log.info("已将流程ID替换为：{}", modelKey);
        }

        validateSequenceFlowRefs(bpmnXml);
        validateBpmnStructure(bpmnXml);
        validateExecutableNodesAndGatewayConditions(bpmnXml);

        try {
            if (repositoryService == null) {
                throw new RuntimeException("Flowable未初始化");
            }
            
            // 如果已发布，增加版本号后重新部署
            int newVersion = model.getVersion();
            if (FlowModelStatus.PUBLISHED.matches(model.getStatus())) {
                // 已发布的模型，版本号+1后重新部署
                newVersion = model.getVersion() + 1;
                log.info("重新部署流程模型：{}，新版本：{}", model.getModelKey(), newVersion);
            } else {
                // 未发布的模型，使用当前版本
                newVersion = model.getVersion() > 0 ? model.getVersion() : 1;
            }
            
            String deploymentKey = model.getModelKey() + "_v" + newVersion;
            String bpmnResourceName = model.getModelKey() + ".bpmn20.xml";
            
            // 生成流程图（使用替换后的 bpmnXml）
            byte[] diagramBytes = null;
            String diagramResourceName = model.getModelKey() + ".png";
            try {
                diagramBytes = generateProcessDiagram(bpmnXml);
                if (diagramBytes != null) {
                    log.info("成功生成流程图：{}，大小：{} bytes", diagramResourceName, diagramBytes.length);
                }
            } catch (Exception e) {
                log.warn("生成流程图失败，将跳过流程图资源：{}", e.getMessage());
            }
            
            // 创建部署（使用替换后的 bpmnXml）
            Deployment deployment;
            if (diagramBytes != null) {
                // 部署 BPMN XML 和流程图
                deployment = repositoryService.createDeployment()
                        .name(model.getModelName())
                        .key(deploymentKey)
                        .addString(bpmnResourceName, bpmnXml)
                        .addBytes(diagramResourceName, diagramBytes)
                        .deploy();
            } else {
                // 仅部署 BPMN XML
                deployment = repositoryService.createDeployment()
                        .name(model.getModelName())
                        .key(deploymentKey)
                        .addString(bpmnResourceName, bpmnXml)
                        .deploy();
            }
            
            // 获取流程定义
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();
            
            // 更新模型状态
            model.setDeploymentId(deployment.getId());
            model.setDeploymentKey(deploymentKey);
            model.setProcessDefinitionId(processDefinition != null ? processDefinition.getId() : null);
            model.setVersion(newVersion);
            model.setBpmnXml(bpmnXml);
            model.setStatus(FlowModelStatus.PUBLISHED.getCode());
            model.setDeployTime(LocalDateTime.now());
            updateById(model);

            eventPublisher.publishEvent(new FlowModelPublishEvent(this, model, changeDescription));
            
            log.info("部署流程模型成功：{}，部署ID：{}", model.getModelKey(), deployment.getId());
            return deployment.getId();
            
        } catch (Exception e) {
            log.error("部署流程模型失败", e);
            throw new RuntimeException("部署失败：" + e.getMessage());
        }
    }
    
    /**
     * 生成流程图
     */
    private byte[] generateProcessDiagram(String bpmnXml) {
        if (processEngineConfiguration == null) {
            log.warn("ProcessEngineConfiguration 未注入，无法生成流程图");
            return null;
        }
        
        try {
            // 打印 BPMN XML 内容用于调试（只打印前500字符）
            if (bpmnXml != null && bpmnXml.length() > 0) {
                String preview = bpmnXml.length() > 500 ? bpmnXml.substring(0, 500) + "..." : bpmnXml;
                log.info("BPMN XML 内容预览: {}", preview);
                log.info("BPMN XML 是否包含 BPMNDiagram: {}", bpmnXml.contains("BPMNDiagram"));
                log.info("BPMN XML 是否包含 bpmndi:BPMNDiagram: {}", bpmnXml.contains("bpmndi:BPMNDiagram"));
            }
            
            // 解析 BPMN XML（第三个参数 true 表示解析图形信息）
            BpmnModel bpmnModel = new org.flowable.bpmn.converter.BpmnXMLConverter()
                    .convertToBpmnModel(new BytesStreamSource(bpmnXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)), false, true);
            
            // 打印解析结果
            log.info("BPMN 模型解析完成，进程数: {}", bpmnModel.getProcesses() != null ? bpmnModel.getProcesses().size() : 0);
            log.info("LocationMap 大小: {}", bpmnModel.getLocationMap() != null ? bpmnModel.getLocationMap().size() : 0);
            log.info("FlowLocationMap 大小: {}", bpmnModel.getFlowLocationMap() != null ? bpmnModel.getFlowLocationMap().size() : 0);
            
            // 检查是否有图形信息
            if (bpmnModel.getLocationMap() == null || bpmnModel.getLocationMap().isEmpty()) {
                log.warn("BPMN 模型没有图形坐标信息，无法生成流程图");
                return null;
            }
            
            // 使用流程图生成器生成图片
            ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
            
            // 设置中文字体
            String activityFontName = "宋体";
            String labelFontName = "宋体";
            String annotationFontName = "宋体";
            
            // 生成流程图（无高亮）
            InputStream diagramStream = diagramGenerator.generateDiagram(
                    bpmnModel,
                    "png",
                    Collections.emptyList(),  // 无高亮已完成节点
                    Collections.emptyList(),  // 无高亮当前节点
                    activityFontName,
                    labelFontName,
                    annotationFontName,
                    null,
                    1.0,
                    true
            );
            
            if (diagramStream != null) {
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = diagramStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                diagramStream.close();
                return output.toByteArray();
            }
            
            return null;
        } catch (Exception e) {
            log.error("生成流程图失败", e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendModel(String id) {
        FlowModel model = getModelByIdAndTenant(id, requireTenantId());
        if (model == null) {
            throw new RuntimeException("流程模型不存在");
        }
        
        if (!FlowModelStatus.PUBLISHED.matches(model.getStatus())) {
            throw new RuntimeException("只有已发布的模型才能挂起");
        }
        
        // 挂起Flowable流程定义
        if (model.getProcessDefinitionId() != null && repositoryService != null) {
            repositoryService.suspendProcessDefinitionById(model.getProcessDefinitionId());
        }
        
        model.setStatus(FlowModelStatus.SUSPENDED.getCode());
        updateById(model);
        log.info("挂起流程模型：{}", model.getModelKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateModel(String id) {
        FlowModel model = getModelByIdAndTenant(id, requireTenantId());
        if (model == null) {
            throw new RuntimeException("流程模型不存在");
        }
        
        if (!FlowModelStatus.SUSPENDED.matches(model.getStatus())) {
            throw new RuntimeException("只有已挂起的模型才能激活");
        }
        
        // 激活Flowable流程定义
        if (model.getProcessDefinitionId() != null && repositoryService != null) {
            repositoryService.activateProcessDefinitionById(model.getProcessDefinitionId());
        }
        
        model.setStatus(FlowModelStatus.PUBLISHED.getCode());
        updateById(model);
        log.info("激活流程模型：{}", model.getModelKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableModel(String id) {
        FlowModel model = getModelByIdAndTenant(id, requireTenantId());
        if (model != null) {
            model.setStatus(FlowModelStatus.DISABLED.getCode());
            updateById(model);
            log.info("禁用流程模型：{}", model.getModelKey());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableModel(String id) {
        FlowModel model = getModelByIdAndTenant(id, requireTenantId());
        if (model != null) {
            model.setStatus(FlowModelStatus.DESIGNING.getCode());
            updateById(model);
            log.info("启用流程模型：{}", model.getModelKey());
        }
    }

    @Override
    public FlowModel getModelDetail(String id) {
        Long tenantId = SessionHelper.getTenantId();
        return tenantId == null || tenantId <= 0 ? null : getModelByIdAndTenant(id, tenantId);
    }

    @Override
    public FlowModel getModelByKey(String modelKey) {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0 || modelKey == null || modelKey.isBlank()) {
            return null;
        }
        return getBaseMapper().selectByModelKeyAndTenantId(modelKey.trim(), tenantId);
    }

    @Override
    public FlowStartConfig getStartConfig(String modelKey) {
        FlowModel model = getModelByKey(modelKey);
        if (model == null) {
            throw new IllegalArgumentException("流程模型不存在：" + modelKey);
        }
        FlowStartConfig config = new FlowStartConfig();
        config.setModelKey(model.getModelKey());
        if (model.getBpmnXml() == null || model.getBpmnXml().isBlank()) {
            return config;
        }
        BpmnModel bpmnModel = new org.flowable.bpmn.converter.BpmnXMLConverter()
                .convertToBpmnModel(new BytesStreamSource(
                        model.getBpmnXml().getBytes(java.nio.charset.StandardCharsets.UTF_8)), false, true);
        config.setInitiatorSelectNodes(InitiatorSelectedApproverSupport.discover(bpmnModel));
        config.setNextNodes(InitiatorSelectedApproverSupport.discoverNextNodes(bpmnModel));
        List<String> diagnostics = new ArrayList<>(
                InitiatorSelectedApproverSupport.preflightNextApprovers(bpmnModel));
        appendCandidateGroupDiagnostics(config.getNextNodes(), diagnostics);
        config.setDiagnostics(diagnostics);
        config.setPreflightPassed(diagnostics.isEmpty());
        return config;
    }

    /**
     * 对静态 candidateGroups 做启动前可解析性检查。表达式形式的候选组依赖
     * 业务变量，留给运行时解析，避免把动态配置误报为不存在。
     */
    private void appendCandidateGroupDiagnostics(List<FlowStartConfig.ApproverNode> nodes,
                                                 List<String> diagnostics) {
        if (flowOrgIntegrationService == null || nodes == null || nodes.isEmpty()) {
            return;
        }
        Set<String> reported = new HashSet<>();
        for (FlowStartConfig.ApproverNode node : nodes) {
            String groups = node == null ? null : node.getCandidateGroups();
            if (!hasText(groups)) {
                continue;
            }
            for (String raw : groups.split("[,;，；]")) {
                String group = raw == null ? "" : raw.trim();
                if (group.isEmpty() || group.contains("${") || group.contains("#{")) {
                    continue;
                }
                try {
                    if (resolveCandidateGroupUsers(group).isEmpty()) {
                        String nodeName = node.getNodeName();
                        String label = hasText(nodeName) ? nodeName : node.getNodeKey();
                        String message = "审批节点「" + (hasText(label) ? label : "未命名")
                                + "」候选组「" + group + "」当前未解析到启用成员";
                        if (reported.add(message)) {
                            diagnostics.add(message);
                        }
                    }
                } catch (RuntimeException exception) {
                    String nodeName = node.getNodeName();
                    String label = hasText(nodeName) ? nodeName : node.getNodeKey();
                    String message = "审批节点「" + (hasText(label) ? label : "未命名")
                            + "」候选组「" + group + "」解析失败，请检查组织配置";
                    if (reported.add(message)) {
                        diagnostics.add(message);
                    }
                }
            }
        }
    }

    private List<String> resolveCandidateGroupUsers(String group) {
        List<String> users = flowOrgIntegrationService.getUserIdsByRoleCode(group);
        if (users != null && !users.isEmpty()) {
            return users;
        }
        if (group.chars().allMatch(Character::isDigit)) {
            users = flowOrgIntegrationService.getUserIdsByRoleId(group);
            if (users != null && !users.isEmpty()) {
                return users;
            }
            users = flowOrgIntegrationService.getUserIdsByDeptId(group);
            if (users != null && !users.isEmpty()) {
                return users;
            }
        }
        users = flowOrgIntegrationService.getUserIdsByGroupCode(group);
        return users == null ? List.of() : users;
    }

    @Override
    public List<FlowModelVersionSummaryVO> getModelVersions(String modelKey) {
        // 查询Flowable中的历史版本
        if (repositoryService == null) {
            return Collections.emptyList();
        }
        
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(modelKey)
                .orderByProcessDefinitionVersion()
                .desc()
                .listPage(0, MAX_MODEL_VERSIONS);
        
        return definitions.stream().map(pd -> {
            FlowModelVersionSummaryVO version = new FlowModelVersionSummaryVO();
            version.setId(pd.getId());
            version.setKey(pd.getKey());
            version.setName(pd.getName());
            version.setVersion(pd.getVersion());
            version.setDeploymentId(pd.getDeploymentId());
            version.setSuspended(pd.isSuspended());
            // 从Deployment获取部署时间
            try {
                if (pd.getDeploymentId() != null) {
                    var deployment = repositoryService.createDeploymentQuery()
                            .deploymentId(pd.getDeploymentId())
                            .singleResult();
                    if (deployment != null) {
                        version.setDeploymentTime(deployment.getDeploymentTime());
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
        bpmnXml = normalizeBpmnXml(bpmnXml, "导入流程模型");
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
        model.setDesignerType("business");
        model.setBpmnXml(bpmnXml);
        model.setStatus(FlowModelStatus.DESIGNING.getCode());
        model.setVersion(1);
        model.setDelFlag(0);
        model.setCreateTime(LocalDateTime.now());
        model.setUpdateTime(LocalDateTime.now());
        model.setTenantId(requireTenantId());
        model.setCreateBy(SessionHelper.getLoginUser().getUsername());
        save(model);
        log.info("导入流程模型成功：{}", model.getModelKey());
        return model;
    }

    @Override
    public String exportModel(String id) {
        FlowModel model = getModelByIdAndTenant(id, requireTenantId());
        if (model == null) {
            throw new RuntimeException("流程模型不存在");
        }
        return model.getBpmnXml();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowModel copyModel(String id, String newName) {
        FlowModel source = getModelByIdAndTenant(id, requireTenantId());
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
        newModel.setDesignerType(normalizeDesignerType(source.getDesignerType()));
        newModel.setFormType(source.getFormType());
        newModel.setFormId(source.getFormId());
        newModel.setFormJson(source.getFormJson());
        newModel.setNotifyType(source.getNotifyType());
        newModel.setWebhookUrl(source.getWebhookUrl());
        newModel.setTodoDetailUrlTemplate(source.getTodoDetailUrlTemplate());
        newModel.setNotifyConfig(source.getNotifyConfig());
        newModel.setAllowMultiReturn(source.getAllowMultiReturn());
        newModel.setBpmnXml(normalizeBpmnXml(source.getBpmnXml(), "复制流程模型"));
        newModel.setStatus(FlowModelStatus.DESIGNING.getCode());
        newModel.setVersion(1);
        newModel.setDelFlag(0);
        newModel.setTenantId(source.getTenantId());
        newModel.setCreateTime(LocalDateTime.now());
        newModel.setCreateBy(SessionHelper.getLoginUser().getUsername());
        newModel.setUpdateTime(LocalDateTime.now());
        
        save(newModel);
        log.info("复制流程模型成功，源模型：{}，新模型：{}", source.getModelKey(), newModel.getModelKey());
        return newModel;
    }

    @Override
    public boolean checkModelKeyExists(String modelKey, String excludeId) {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0 || modelKey == null || modelKey.isBlank()) {
            return false;
        }
        return getBaseMapper().countByModelKeyAndTenantId(modelKey.trim(), tenantId, excludeId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortModels(FlowModelSortDTO request) {
        Long tenantId = requireTenantId();
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(400, "排序项不能为空");
        }
        if (request.getItems().size() > 200) {
            throw new BusinessException(400, "单次最多调整200个流程模型");
        }
        LinkedHashMap<String, Integer> requested = new LinkedHashMap<>();
        for (FlowModelSortItemDTO item : request.getItems()) {
            if (item == null || item.getModelId() == null || item.getModelId().isBlank()) {
                throw new BusinessException(400, "模型ID不能为空");
            }
            if (item.getSortOrder() == null || item.getSortOrder() < 0 || item.getSortOrder() > 1_000_000) {
                throw new BusinessException(400, "排序值必须在0到1000000之间");
            }
            String modelId = item.getModelId().trim();
            if (requested.putIfAbsent(modelId, item.getSortOrder()) != null) {
                throw new BusinessException(400, "排序项中存在重复模型");
            }
        }
        List<FlowModel> locked = getBaseMapper().selectByIdsForUpdate(new ArrayList<>(requested.keySet()), tenantId);
        if (locked.size() != requested.size()) {
            throw new BusinessException(404, "排序列表包含不存在或无权访问的流程模型");
        }
        String operator = SessionHelper.getUsername();
        if (operator == null || operator.isBlank()) {
            throw new BusinessException(403, "无法确定当前操作人");
        }
        for (Map.Entry<String, Integer> entry : requested.entrySet()) {
            if (getBaseMapper().updateSortOrder(entry.getKey(), tenantId, entry.getValue(), operator) != 1) {
                throw new BusinessException(409, "流程模型排序已被其他请求修改，请刷新后重试");
            }
        }
        log.info("批量调整流程模型排序: tenantId={}, count={}", tenantId, requested.size());
    }

    private Long requireTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("缺少可信租户上下文");
        }
        return tenantId;
    }

    private FlowModel getModelByIdAndTenant(String id, Long tenantId) {
        if (id == null || id.isBlank() || tenantId == null || tenantId <= 0) {
            return null;
        }
        return getBaseMapper().selectByIdAndTenant(id.trim(), tenantId);
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
    
    /**
     * 将 BPMN XML 中的 process id 替换为 modelKey
     * 这样启动流程时使用 modelKey 就能找到正确的流程定义
     */
    private String replaceProcessId(String bpmnXml, String modelKey) {
        try {
            // 提取当前的 process id
            String currentProcessId = extractProcessKey(bpmnXml);
            if (currentProcessId == null) {
                log.warn("无法提取当前流程ID，跳过替换");
                return bpmnXml;
            }
            
            if (currentProcessId.equals(modelKey)) {
                log.debug("流程ID已经是 {}，无需替换", modelKey);
                return bpmnXml;
            }
            
            log.info("将流程ID从 {} 替换为 {}", currentProcessId, modelKey);
            
            // 替换 process id（需要替换多个地方）
            // 1. <bpmn:process id="xxx" 或 <process id="xxx"
            // 2. bpmnElement="xxx" 在 BPMNPlane 中
            // 3. 可能还有其他引用
            
            // 替换 <bpmn:process id="xxx"
            bpmnXml = bpmnXml.replace(
                    "<bpmn:process id=\"" + currentProcessId + "\"",
                    "<bpmn:process id=\"" + modelKey + "\"");
            
            // 替换 <process id="xxx"（无命名空间的情况）
            bpmnXml = bpmnXml.replace(
                    "<process id=\"" + currentProcessId + "\"",
                    "<process id=\"" + modelKey + "\"");
            
            // 替换 BPMNPlane 中的 bpmnElement
            bpmnXml = bpmnXml.replace(
                    "bpmnElement=\"" + currentProcessId + "\"",
                    "bpmnElement=\"" + modelKey + "\"");
            
            return bpmnXml;
        } catch (Exception e) {
            log.warn("替换流程ID失败", e);
            return bpmnXml;
        }
    }
    
    private void validateSequenceFlowRefs(String bpmnXml) {
        java.util.regex.Pattern flowPattern = java.util.regex.Pattern.compile(
                "<(?:bpmn:)?sequenceFlow\\b([^>]*?)/?>",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Pattern idPattern = java.util.regex.Pattern.compile("\\bid=\"([^\"]*)\"");
        java.util.regex.Pattern targetRefPattern = java.util.regex.Pattern.compile("\\btargetRef=\"([^\"]*)\"");
        java.util.regex.Pattern sourceRefPattern = java.util.regex.Pattern.compile("\\bsourceRef=\"([^\"]*)\"");
        
        java.util.regex.Matcher matcher = flowPattern.matcher(bpmnXml);
        while (matcher.find()) {
            String flowElement = matcher.group(0);
            String attrs = matcher.group(1);
            
            java.util.regex.Matcher idMatcher = idPattern.matcher(flowElement);
            String flowId = idMatcher.find() ? idMatcher.group(1) : "unknown";
            
            boolean hasTargetRef = targetRefPattern.matcher(attrs).find();
            boolean hasSourceRef = sourceRefPattern.matcher(attrs).find();
            
            if (!hasTargetRef || !hasSourceRef) {
                String missing = !hasTargetRef ? "targetRef" : "sourceRef";
                log.error("BPMN sequenceFlow [{}] 缺少 {} 属性，XML 片段: {}",
                        flowId, missing, flowElement);
                throw new RuntimeException(String.format(
                        "流程图数据不完整：连线 [%s] 缺少 %s 属性。" +
                        "请在流程设计器中检查所有连线是否完整连接到目标节点，重新保存后再部署。",
                        flowId, missing));
            }
        }
    }

    /**
     * 部署前校验最小可执行结构，避免模型在设计器中可保存、但启动时才发现没有入口/出口或悬空连线。
     * 这里不强制 userTask 必须写死 assignee：Forge 支持节点配置、表达式和运行时组织解析。
     */
    private void validateBpmnStructure(String bpmnXml) {
        if (countMatches(bpmnXml, "<(?:bpmn:)?startEvent\\b") == 0) {
            throw new RuntimeException("流程模型缺少开始节点，请至少配置一个开始节点。");
        }
        if (countMatches(bpmnXml, "<(?:bpmn:)?endEvent\\b") == 0) {
            throw new RuntimeException("流程模型缺少结束节点，请至少配置一个结束节点。");
        }

        Set<String> nodeIds = new HashSet<>();
        java.util.regex.Matcher nodeMatcher = java.util.regex.Pattern.compile(
                "<(?:bpmn:)?(?:startEvent|endEvent|userTask|serviceTask|scriptTask|exclusiveGateway|parallelGateway|inclusiveGateway|callActivity|subProcess)\\b([^>]*)>",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(bpmnXml);
        java.util.regex.Pattern idPattern = java.util.regex.Pattern.compile("\\bid=\"([^\"]+)\"");
        while (nodeMatcher.find()) {
            java.util.regex.Matcher idMatcher = idPattern.matcher(nodeMatcher.group(1));
            if (idMatcher.find()) {
                nodeIds.add(idMatcher.group(1));
            }
        }

        java.util.regex.Matcher flowMatcher = java.util.regex.Pattern.compile(
                "<(?:bpmn:)?sequenceFlow\\b([^>]*)/?>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(bpmnXml);
        while (flowMatcher.find()) {
            String attrs = flowMatcher.group(1);
            String source = attributeValue(attrs, "sourceRef");
            String target = attributeValue(attrs, "targetRef");
            if (source == null || target == null || !nodeIds.contains(source) || !nodeIds.contains(target)) {
                throw new RuntimeException("流程模型存在悬空连线，请检查 sourceRef 和 targetRef 是否指向有效节点。");
            }
        }
    }

    /**
     * Reject BPMN elements that the Forge runtime does not execute and catch
     * empty approval/gateway configuration while the model is still editable.
     */
    private void validateExecutableNodesAndGatewayConditions(String bpmnXml) {
        java.util.regex.Pattern nodePattern = java.util.regex.Pattern.compile(
                "<(?:bpmn:)?(scriptTask|callActivity|subProcess|serviceTask|userTask)\\b([^>]*)>",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher nodeMatcher = nodePattern.matcher(bpmnXml);
        while (nodeMatcher.find()) {
            String type = nodeMatcher.group(1).toLowerCase(Locale.ROOT);
            String attrs = nodeMatcher.group(2);
            String id = attributeValue(attrs, "id");
            String name = attributeValue(attrs, "name");
            String label = (name == null || name.isBlank()) ? id : name;
            if ("scriptTask".equalsIgnoreCase(type)
                    || "callActivity".equalsIgnoreCase(type)
                    || "subProcess".equalsIgnoreCase(type)) {
                throw new RuntimeException(String.format(
                        "节点 [%s] 使用了暂不支持的执行类型 [%s]，请改用用户任务或受支持的抄送节点。",
                        label == null ? "未命名" : label, type));
            }
            if ("serviceTask".equalsIgnoreCase(type)
                    && !"cc".equalsIgnoreCase(attributeValue(attrs, "type"))) {
                throw new RuntimeException(String.format(
                        "节点 [%s] 的 serviceTask 未声明受支持的 flowable:type=cc，无法保证运行时执行委托。",
                        label == null ? "未命名" : label));
            }
            if ("serviceTask".equalsIgnoreCase(type)
                    && containsUnsupportedExecutionAttribute(attrs)) {
                throw new RuntimeException(String.format(
                        "节点 [%s] 包含未注册的执行委托属性，禁止通过 raw XML 绕过执行白名单。",
                        label == null ? "未命名" : label));
            }
            if ("userTask".equalsIgnoreCase(type)) {
                boolean hasAssignee = hasText(attributeValue(attrs, "assignee"));
                boolean hasCandidateUsers = hasText(attributeValue(attrs, "candidateUsers"));
                boolean hasCandidateGroups = hasText(attributeValue(attrs, "candidateGroups"));
                if (!hasAssignee && !hasCandidateUsers && !hasCandidateGroups) {
                    throw new RuntimeException(String.format(
                            "审批节点 [%s] 未配置处理人、候选用户或候选组，请先完成审批人配置。",
                            label == null ? "未命名" : label));
                }
            }
        }

        java.util.regex.Pattern gatewayPattern = java.util.regex.Pattern.compile(
                "<(?:bpmn:)?(exclusiveGateway|inclusiveGateway)\\b([^>]*)>([\\s\\S]*?)</(?:bpmn:)?\\1>",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Pattern flowPattern = java.util.regex.Pattern.compile(
                "<(?:bpmn:)?sequenceFlow\\b([^>]*)>([\\s\\S]*?)</(?:bpmn:)?sequenceFlow>|"
                        + "<(?:bpmn:)?sequenceFlow\\b([^>]*)/>",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher gatewayMatcher = gatewayPattern.matcher(bpmnXml);
        while (gatewayMatcher.find()) {
            String gatewayAttrs = gatewayMatcher.group(2);
            String gatewayId = attributeValue(gatewayAttrs, "id");
            if (gatewayId == null) {
                continue;
            }
            String defaultFlow = attributeValue(gatewayAttrs, "default");
            int outgoingCount = 0;
            java.util.regex.Matcher flowMatcher = flowPattern.matcher(bpmnXml);
            while (flowMatcher.find()) {
                String flowAttrs = flowMatcher.group(1) != null ? flowMatcher.group(1) : flowMatcher.group(3);
                if (!gatewayId.equals(attributeValue(flowAttrs, "sourceRef"))) {
                    continue;
                }
                outgoingCount++;
                String flowId = attributeValue(flowAttrs, "id");
                boolean hasCondition = flowMatcher.group(2) != null
                        && flowMatcher.group(2).toLowerCase(Locale.ROOT).contains("conditionexpression");
                if (!hasCondition && !Objects.equals(defaultFlow, flowId)) {
                    throw new RuntimeException(String.format(
                            "网关 [%s] 的分支连线 [%s] 缺少条件表达式或默认分支。",
                            gatewayId, flowId == null ? "未命名" : flowId));
                }
            }
            if (outgoingCount > 1 && defaultFlow == null) {
                log.debug("网关 [{}] 未声明 default，但所有出口均已配置条件表达式", gatewayId);
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean containsUnsupportedExecutionAttribute(String attributes) {
        String normalized = attributes == null ? "" : attributes.toLowerCase(Locale.ROOT);
        return normalized.contains("flowable:class=")
                || normalized.contains("flowable:delegateexpression=")
                || normalized.contains("flowable:expression=")
                || normalized.contains("activiti:class=")
                || normalized.contains("activiti:delegateexpression=")
                || normalized.contains("activiti:expression=");
    }

    private int countMatches(String value, String regex) {
        int count = 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex,
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(value);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String attributeValue(String attributes, String name) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "\\b" + name + "=\"([^\"]*)\"").matcher(attributes);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void validateNoProcessData(FlowModel model) {
        String modelKey = model.getModelKey();
        if (modelKey == null || modelKey.trim().isEmpty()) {
            return;
        }

        long businessCount = flowBusinessMapper.selectCount(new LambdaQueryWrapper<FlowBusiness>()
                .eq(FlowBusiness::getProcessDefKey, modelKey));
        long runningCount = 0L;
        if (runtimeService != null) {
            runningCount = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(modelKey)
                    .count();
        }
        long historyCount = 0L;
        if (historyService != null) {
            historyCount = historyService.createHistoricProcessInstanceQuery()
                    .processDefinitionKey(modelKey)
                    .count();
        }

        if (businessCount > 0 || runningCount > 0 || historyCount > 0) {
            throw new BusinessException(400, String.format(
                    "流程模型「%s」下已有流程数据（业务记录 %d 条、运行中实例 %d 条、历史实例 %d 条），请先在流程监控中清理相关流程后再删除模型",
                    model.getModelName() != null ? model.getModelName() : modelKey,
                    businessCount,
                    runningCount,
                    historyCount));
        }
    }

    private String normalizeBpmnXml(String bpmnXml, String operation) {
        BpmnXmlUtils.NormalizationResult result = BpmnXmlUtils.normalizeDuplicateSequenceFlows(bpmnXml);
        if (result.hasRepairs()) {
            String repairSummary = result.getRepairs().stream()
                    .map(repair -> String.format("%s->%s 保留 [%s] 删除 %s",
                            repair.getSourceRef(),
                            repair.getTargetRef(),
                            repair.getKeptFlowId(),
                            repair.getRemovedFlowIds()))
                    .collect(Collectors.joining("; "));
            log.warn("{}：已自动清理 BPMN 重复连线，{}", operation, repairSummary);
        }
        BpmnXmlUtils.LegacyMultiInstanceNormalizationResult multiInstanceResult =
                BpmnXmlUtils.normalizeLegacyMultiInstanceExpressions(result.getBpmnXml());
        if (multiInstanceResult.hasRepairs()) {
            log.warn("{}：已自动清理 BPMN 旧版会签表达式，节点={}",
                    operation, multiInstanceResult.getNormalizedNodeIds());
        }
        return multiInstanceResult.getBpmnXml();
    }
}
