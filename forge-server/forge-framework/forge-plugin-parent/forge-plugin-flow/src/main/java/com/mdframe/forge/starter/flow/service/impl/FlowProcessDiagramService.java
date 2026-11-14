package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.dto.ProcessDiagramInfo;
import com.mdframe.forge.starter.flow.dto.ProcessNodeInfo;
import com.mdframe.forge.starter.flow.dto.ProcessSequenceFlowInfo;
import com.mdframe.forge.starter.flow.enums.FlowDiagramStatus;
import com.mdframe.forge.starter.flow.security.FlowAccessGuard;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程图读取与渲染服务。
 *
 * <p>使用 Facade + Assembler 边界集中 BPMN XML/PNG、节点状态、连线状态和人员展示组装，
 * 不参与任务动作或流程状态迁移。</p>
 */
@Slf4j
final class FlowProcessDiagramService {

    private static final int MAX_DETAIL_HISTORY_ITEMS = 1000;

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final ProcessEngineConfiguration processEngineConfiguration;
    private final FlowOrgIntegrationService flowOrgIntegrationService;
    private final FlowAccessGuard flowAccessGuard;

    FlowProcessDiagramService(RuntimeService runtimeService,
                              TaskService taskService,
                              RepositoryService repositoryService,
                              HistoryService historyService,
                              ProcessEngineConfiguration processEngineConfiguration,
                              FlowOrgIntegrationService flowOrgIntegrationService,
                              FlowAccessGuard flowAccessGuard) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.repositoryService = repositoryService;
        this.historyService = historyService;
        this.processEngineConfiguration = processEngineConfiguration;
        this.flowOrgIntegrationService = flowOrgIntegrationService;
        this.flowAccessGuard = flowAccessGuard;
    }

    byte[] getProcessDiagram(String processInstanceId) {
        try {
            flowAccessGuard.requireProcessVisible(processInstanceId);
            // 1. 获取流程实例
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            
            if (historicProcessInstance == null) {
                log.warn("流程实例不存在：{}", processInstanceId);
                return null;
            }
            
            String processDefinitionId = historicProcessInstance.getProcessDefinitionId();
            log.info("获取流程图：processInstanceId={}, processDefinitionId={}", processInstanceId, processDefinitionId);
            
            // 2. 获取BPMN模型
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) {
                log.warn("BPMN模型不存在：{}", processDefinitionId);
                return null;
            }
            
            // 打印BPMN模型信息
            log.info("BPMN模型进程数: {}", bpmnModel.getProcesses() != null ? bpmnModel.getProcesses().size() : 0);
            log.info("BPMN模型LocationMap大小: {}", bpmnModel.getLocationMap() != null ? bpmnModel.getLocationMap().size() : 0);
            log.info("BPMN模型FlowLocationMap大小: {}", bpmnModel.getFlowLocationMap() != null ? bpmnModel.getFlowLocationMap().size() : 0);
            
            // 3. 检查BPMN模型是否有图形信息
            if (!hasGraphicInfo(bpmnModel)) {
                log.info("BPMN模型没有图形坐标信息，尝试从部署资源获取原始流程图");
                return getDiagramFromResource(processDefinitionId);
            }
            
            // 4. 获取已完成的历史活动节点
            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .finished()
                    .orderByHistoricActivityInstanceStartTime()
                    .asc()
                    .listPage(0, MAX_DETAIL_HISTORY_ITEMS);
            
            // 已完成的节点ID列表
            List<String> completedActivityIds = historicActivityInstances.stream()
                    .map(HistoricActivityInstance::getActivityId)
                    .distinct()
                    .collect(Collectors.toList());
            
            log.info("已完成节点数量: {}, 节点ID: {}", completedActivityIds.size(), completedActivityIds);
            
            // 5. 获取当前活动节点（运行中）
            List<String> currentActivityIds = new ArrayList<>();
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            
            if (processInstance != null) {
                // 流程还在运行中，获取当前活动节点
                currentActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
                log.info("当前活动节点数量: {}, 节点ID: {}", currentActivityIds.size(), currentActivityIds);
            }
            
            // 6. 使用流程图生成器生成图片
            ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
            
            if (diagramGenerator == null) {
                log.error("ProcessDiagramGenerator 未配置");
                return null;
            }
            
            // 设置字体（使用系统默认字体，避免字体不存在的问题）
            String activityFontName = "SansSerif";
            String labelFontName = "SansSerif";
            String annotationFontName = "SansSerif";
            
            log.info("开始生成流程图，字体: {}", activityFontName);
            
            // 生成流程图输入流（高亮已完成和当前节点）
            InputStream diagramStream = diagramGenerator.generateDiagram(
                    bpmnModel,
                    "png",
                    completedActivityIds,    // 高亮已完成节点（绿色）
                    currentActivityIds,      // 高亮当前节点（红色）
                    activityFontName,
                    labelFontName,
                    annotationFontName,
                    null,
                    1.0,
                    true
            );
            
            // 7. 转换为字节数组
            if (diagramStream != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = diagramStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                diagramStream.close();
                log.info("流程图生成成功，大小: {} bytes", output.size());
                return output.toByteArray();
            }
            
            log.warn("流程图生成返回空流");
            return null;
            
        } catch (Exception e) {
            log.error("生成流程图失败：processInstanceId={}, 错误: {}", processInstanceId, e.getMessage(), e);
            throw new RuntimeException("生成流程图失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取流程定义的 BPMN XML
     */
    private String getBpmnXml(ProcessDefinition processDefinition) {
        try {
            // 获取 BpmnModel
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
            if (bpmnModel == null) {
                log.warn("BPMN模型不存在：{}", processDefinition.getId());
                return null;
            }
            
            // 使用 Flowable 的 XML 导出功能
            byte[] bpmnBytes = new org.flowable.bpmn.converter.BpmnXMLConverter()
                    .convertToXML(bpmnModel);
            
            return new String(bpmnBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("获取BPMN XML失败：processDefinitionId={}", processDefinition.getId(), e);
            return null;
        }
    }
    
    /**
     * 检查BPMN模型是否有图形信息
     */
    private boolean hasGraphicInfo(BpmnModel bpmnModel) {
        if (bpmnModel.getLocationMap() == null || bpmnModel.getLocationMap().isEmpty()) {
            return false;
        }
        // 检查是否有有效的坐标信息
        for (org.flowable.bpmn.model.GraphicInfo graphicInfo : bpmnModel.getLocationMap().values()) {
            if (graphicInfo != null && graphicInfo.getX() >= 0 && graphicInfo.getY() >= 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 从资源获取原始流程图（无高亮）
     */
    private byte[] getDiagramFromResource(String processDefinitionId) {
        try {
            // 获取流程定义
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            
            if (processDefinition == null) {
                log.warn("流程定义不存在：{}", processDefinitionId);
                return null;
            }
            
            log.info("流程定义信息：id={}, name={}, deploymentId={}, diagramResourceName={}",
                    processDefinition.getId(),
                    processDefinition.getName(),
                    processDefinition.getDeploymentId(),
                    processDefinition.getDiagramResourceName());
            
            // 获取流程图资源
            String diagramResourceName = processDefinition.getDiagramResourceName();
            if (diagramResourceName != null && !diagramResourceName.isEmpty()) {
                log.info("尝试从部署资源获取流程图：{}", diagramResourceName);
                try {
                    // 从资源流获取流程图
                    InputStream diagramStream = repositoryService.getResourceAsStream(
                            processDefinition.getDeploymentId(), diagramResourceName);
                    
                    if (diagramStream != null) {
                        // 转换为字节数组
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = diagramStream.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                        diagramStream.close();
                        log.info("成功从部署资源获取流程图，大小：{} bytes", output.size());
                        return output.toByteArray();
                    }
                } catch (Exception e) {
                    log.warn("从部署资源获取流程图失败：{}", e.getMessage());
                }
            }
            
            // 如果没有流程图资源，尝试从 BPMN XML 重新生成
            log.info("尝试从 BPMN XML 资源重新生成流程图");
            return generateDiagramFromBpmnXml(processDefinition);
            
        } catch (Exception e) {
            log.error("从资源获取流程图失败：processDefinitionId={}", processDefinitionId, e);
            return null;
        }
    }
    
    /**
     * 从 BPMN XML 资源重新生成流程图
     */
    private byte[] generateDiagramFromBpmnXml(ProcessDefinition processDefinition) {
        try {
            // 获取 BPMN XML 资源名称
            String resourceName = processDefinition.getResourceName();
            log.info("BPMN XML 资源名称：{}", resourceName);
            
            // 获取 BPMN XML 内容
            InputStream bpmnStream = repositoryService.getResourceAsStream(
                    processDefinition.getDeploymentId(), resourceName);
            
            if (bpmnStream == null) {
                log.warn("无法获取 BPMN XML 资源：{}", resourceName);
                return null;
            }
            
            // 读取 BPMN XML 内容
            ByteArrayOutputStream bpmnOutput = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bpmnStream.read(buffer)) != -1) {
                bpmnOutput.write(buffer, 0, bytesRead);
            }
            bpmnStream.close();
            
            String bpmnXml = bpmnOutput.toString("UTF-8");
            log.info("BPMN XML 内容长度：{}", bpmnXml.length());
            log.info("BPMN XML 是否包含 BPMNDiagram：{}", bpmnXml.contains("BPMNDiagram"));
            
            // 使用 BpmnXMLConverter 解析（第三个参数 true 表示解析图形信息）
            BpmnModel bpmnModel = new org.flowable.bpmn.converter.BpmnXMLConverter()
                    .convertToBpmnModel(
                            new org.flowable.common.engine.impl.util.io.BytesStreamSource(
                                    bpmnXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                            false,
                            true);
            
            log.info("重新解析后的 LocationMap 大小：{}",
                    bpmnModel.getLocationMap() != null ? bpmnModel.getLocationMap().size() : 0);
            
            // 检查是否有图形信息
            if (bpmnModel.getLocationMap() == null || bpmnModel.getLocationMap().isEmpty()) {
                log.warn("BPMN XML 中没有图形坐标信息");
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
                    java.util.Collections.emptyList(),
                    java.util.Collections.emptyList(),
                    activityFontName,
                    labelFontName,
                    annotationFontName,
                    null,
                    1.0,
                    true
            );
            
            if (diagramStream != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                buffer = new byte[4096];
                while ((bytesRead = diagramStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                diagramStream.close();
                log.info("成功从 BPMN XML 生成流程图，大小：{} bytes", output.size());
                return output.toByteArray();
            }
            
            return null;
        } catch (Exception e) {
            log.error("从 BPMN XML 生成流程图失败", e);
            return null;
        }
    }

    ProcessDiagramInfo getProcessDiagramInfo(String processInstanceId) {
        return getProcessDiagramInfo(processInstanceId, false);
    }

    ProcessDiagramInfo getProcessDiagramInfo(String processInstanceId, boolean includeImage) {
        try {
            flowAccessGuard.requireProcessVisible(processInstanceId);
            log.info("开始获取流程图详情，processInstanceId: {}", processInstanceId);
            
            // 1. 获取流程实例
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            
            if (historicProcessInstance == null) {
                log.warn("流程实例不存在：{}", processInstanceId);
                return null;
            }
            
            String processDefinitionId = historicProcessInstance.getProcessDefinitionId();
            log.info("流程定义ID: {}", processDefinitionId);
            
            // 2. 获取BPMN模型
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) {
                log.warn("BPMN模型不存在：{}", processDefinitionId);
                return null;
            }
            
            // 3. 构建返回结果
            ProcessDiagramInfo diagramInfo = new ProcessDiagramInfo();
            diagramInfo.setProcessInstanceId(processInstanceId);
            diagramInfo.setProcessDefinitionId(processDefinitionId);
            // 发起时未设置实例名称时 getName() 为空，回退到流程定义名称
            diagramInfo.setProcessName(historicProcessInstance.getName() != null && !historicProcessInstance.getName().isBlank()
                    ? historicProcessInstance.getName()
                    : historicProcessInstance.getProcessDefinitionName());
            diagramInfo.setStartUserId(historicProcessInstance.getStartUserId());
            diagramInfo.setStartTime(historicProcessInstance.getStartTime());
            diagramInfo.setEndTime(historicProcessInstance.getEndTime());
            
            // 获取发起人姓名
            String startUserId = historicProcessInstance.getStartUserId();
            if (startUserId != null && flowOrgIntegrationService != null) {
                try {
                    Map<String, Object> userInfo = flowOrgIntegrationService.getUserInfo(startUserId);
                    diagramInfo.setStartUserName((String)userInfo.get("realName"));
                    log.info("发起人姓名: {}", (String)userInfo.get("realName"));
                } catch (Exception e) {
                    log.warn("获取发起人姓名失败: {}", e.getMessage());
                }
            }
            
            // 判断流程状态
            if (historicProcessInstance.getEndTime() == null) {
                diagramInfo.setStatus(FlowDiagramStatus.RUNNING.getCode());
            } else if (historicProcessInstance.getDeleteReason() != null) {
                diagramInfo.setStatus(FlowDiagramStatus.TERMINATED.getCode());
            } else {
                diagramInfo.setStatus(FlowDiagramStatus.COMPLETED.getCode());
            }
            log.info("流程状态: {}", diagramInfo.getStatus());
            
            // 4. 获取 BPMN XML（用于前端 bpmn-js 渲染）
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            if (processDefinition != null) {
                String bpmnXml = getBpmnXml(processDefinition);
                diagramInfo.setBpmnXml(bpmnXml);
                log.info("BPMN XML 长度: {}", bpmnXml != null ? bpmnXml.length() : 0);
            }
            
            // 5. 生成流程图图片（备用，默认关闭。BPMN XML 已足够前端渲染，避免每次生成 Base64 PNG 拖慢加载）
            if (includeImage) {
                byte[] diagramBytes = getProcessDiagram(processInstanceId);
                if (diagramBytes != null && diagramBytes.length > 0) {
                    String base64 = Base64.getEncoder().encodeToString(diagramBytes);
                    diagramInfo.setDiagramBase64("data:image/png;base64," + base64);
                    log.info("流程图图片大小: {} bytes, base64长度: {}", diagramBytes.length, base64.length());
                } else {
                    log.warn("未能生成流程图图片");
                }
            }
            
            // 6. 获取节点信息列表
            List<ProcessNodeInfo> nodes = buildNodeInfoList(bpmnModel, processInstanceId);
            diagramInfo.setNodes(nodes);
            log.info("节点数量: {}", nodes != null ? nodes.size() : 0);

            SequenceFlowStatus sequenceFlowStatus = buildSequenceFlowInfoList(bpmnModel, processInstanceId);
            diagramInfo.setSequenceFlows(sequenceFlowStatus.flows());
            diagramInfo.setSequenceFlowStatusAvailable(sequenceFlowStatus.available());
            diagramInfo.setSequenceFlowStatusMessage(sequenceFlowStatus.message());
            
            // 打印节点详情
            if (nodes != null && !nodes.isEmpty()) {
                for (ProcessNodeInfo node : nodes) {
                    log.info("节点: id={}, name={}, type={}, status={}, x={}, y={}",
                            node.getNodeId(), node.getNodeName(), node.getNodeType(),
                            node.getStatus(), node.getX(), node.getY());
                }
            }
            
            return diagramInfo;
            
        } catch (Exception e) {
            log.error("获取流程图详情失败：processInstanceId={}", processInstanceId, e);
            return null;
        }
    }

    /**
     * 从 Flowable 历史活动中批量计算连线执行状态。只有历史级别记录过
     * sequenceFlow 活动时才宣称状态可靠，避免把“没有记录”误报为所有连线未执行。
     */
    private SequenceFlowStatus buildSequenceFlowInfoList(BpmnModel bpmnModel, String processInstanceId) {
        if (bpmnModel == null || bpmnModel.getProcesses() == null || bpmnModel.getProcesses().isEmpty()) {
            return new SequenceFlowStatus(List.of(), false, "BPMN 模型缺失，无法计算连线状态");
        }
        Process process = bpmnModel.getProcesses().get(0);
        List<SequenceFlow> definitions = process.findFlowElementsOfType(SequenceFlow.class);
        if (definitions == null || definitions.isEmpty()) {
            return new SequenceFlowStatus(List.of(), true, null);
        }
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .listPage(0, MAX_DETAIL_HISTORY_ITEMS);
        Set<String> executedFlowIds = activities == null ? Set.of() : activities.stream()
                .filter(activity -> activity != null && activity.getEndTime() != null)
                .filter(activity -> "sequenceFlow".equalsIgnoreCase(activity.getActivityType()))
                .map(HistoricActivityInstance::getActivityId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        if (executedFlowIds.isEmpty()) {
            return new SequenceFlowStatus(List.of(), false,
                    "当前 Flowable 历史级别未记录 sequenceFlow 活动，连线状态不可用");
        }
        List<ProcessSequenceFlowInfo> result = definitions.stream().map(flow -> {
            ProcessSequenceFlowInfo item = new ProcessSequenceFlowInfo();
            item.setFlowId(flow.getId());
            item.setSourceRef(flow.getSourceRef());
            item.setTargetRef(flow.getTargetRef());
            item.setStatus(executedFlowIds.contains(flow.getId())
                    ? FlowDiagramStatus.COMPLETED.getCode() : FlowDiagramStatus.PENDING.getCode());
            return item;
        }).toList();
        return new SequenceFlowStatus(result, true, null);
    }

    private record SequenceFlowStatus(List<ProcessSequenceFlowInfo> flows,
                                      boolean available, String message) {
    }
    
    /**
     * 构建节点信息列表
     */
    private List<ProcessNodeInfo> buildNodeInfoList(BpmnModel bpmnModel, String processInstanceId) {
        List<ProcessNodeInfo> nodeList = new ArrayList<>();
        
        // 获取流程中的所有节点
        org.flowable.bpmn.model.Process process = bpmnModel.getProcesses().get(0);
        if (process == null) {
            return nodeList;
        }
        
        // 获取已完成的历史活动
        Map<String, HistoricActivityInstance> completedActivityMap = new HashMap<>();
        List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .listPage(0, MAX_DETAIL_HISTORY_ITEMS);
        
        for (HistoricActivityInstance activity : historicActivities) {
            if (activity.getEndTime() != null) {
                // 已完成的活动
                if (!completedActivityMap.containsKey(activity.getActivityId()) ||
                    completedActivityMap.get(activity.getActivityId()).getStartTime().before(activity.getStartTime())) {
                    completedActivityMap.put(activity.getActivityId(), activity);
                }
            }
        }
        
        // 获取当前活动节点
        Set<String> currentActivityIds = new HashSet<>();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        
        if (processInstance != null) {
            currentActivityIds = new HashSet<>(runtimeService.getActiveActivityIds(processInstanceId));
        }
        
        // 获取历史任务信息（用于获取处理人）
        Map<String, HistoricTaskInstance> taskMap = new HashMap<>();
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .listPage(0, MAX_DETAIL_HISTORY_ITEMS);
        for (HistoricTaskInstance task : historicTasks) {
            if (!taskMap.containsKey(task.getTaskDefinitionKey()) ||
                taskMap.get(task.getTaskDefinitionKey()).getCreateTime().before(task.getCreateTime())) {
                taskMap.put(task.getTaskDefinitionKey(), task);
            }
        }
        
        // 获取当前任务
        Map<String, Task> currentTaskMap = new HashMap<>();
        List<Task> currentTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        for (Task task : currentTasks) {
            currentTaskMap.put(task.getTaskDefinitionKey(), task);
        }

        // 同一流程图内的处理人和候选人可能跨多个节点重复出现，复用一次批量查询结果，
        // 避免按节点、按用户触发组织服务 N+1 回查。
        Map<String, Map<String, Object>> userInfoCache = new HashMap<>();
        if (flowOrgIntegrationService != null) {
            Set<String> userIdsToLoad = new LinkedHashSet<>();
            currentTaskMap.values().forEach(task -> {
                if (task.getAssignee() != null && !task.getAssignee().isBlank()) {
                    userIdsToLoad.add(task.getAssignee());
                }
                taskService.getIdentityLinksForTask(task.getId()).stream()
                        .filter(link -> link.getUserId() != null && "candidate".equals(link.getType()))
                        .map(org.flowable.identitylink.api.IdentityLink::getUserId)
                        .forEach(userIdsToLoad::add);
            });
            historicTasks.stream()
                    .map(HistoricTaskInstance::getAssignee)
                    .filter(id -> id != null && !id.isBlank())
                    .forEach(userIdsToLoad::add);
            if (!userIdsToLoad.isEmpty()) {
                Map<String, Map<String, Object>> loaded =
                        flowOrgIntegrationService.getUserInfoBatch(new ArrayList<>(userIdsToLoad));
                if (loaded != null) {
                    userInfoCache.putAll(loaded);
                }
                userIdsToLoad.forEach(id -> userInfoCache.putIfAbsent(id, Collections.emptyMap()));
            }
        }
        
        // 遍历所有节点
        for (FlowNode flowNode : process.findFlowElementsOfType(FlowNode.class)) {
            ProcessNodeInfo nodeInfo = new ProcessNodeInfo();
            nodeInfo.setNodeId(flowNode.getId());
            nodeInfo.setNodeName(flowNode.getName());
            nodeInfo.setNodeType(flowNode.getClass().getSimpleName());
            
            // 获取图形信息
            GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());
            if (graphicInfo != null) {
                nodeInfo.setX(graphicInfo.getX());
                nodeInfo.setY(graphicInfo.getY());
                nodeInfo.setWidth(graphicInfo.getWidth());
                nodeInfo.setHeight(graphicInfo.getHeight());
            }
            
            // 设置节点状态
            if (currentActivityIds.contains(flowNode.getId())) {
                nodeInfo.setStatus(FlowDiagramStatus.RUNNING.getCode());
            } else if (completedActivityMap.containsKey(flowNode.getId())) {
                nodeInfo.setStatus(FlowDiagramStatus.COMPLETED.getCode());
                HistoricActivityInstance activity = completedActivityMap.get(flowNode.getId());
                nodeInfo.setStartTime(activity.getStartTime());
                nodeInfo.setEndTime(activity.getEndTime());
                if (activity.getDurationInMillis() != null) {
                    nodeInfo.setDuration(activity.getDurationInMillis());
                }
            } else {
                nodeInfo.setStatus(FlowDiagramStatus.PENDING.getCode());
            }
            
            // 设置处理人信息（仅用户任务）
            if ("UserTask".equals(nodeInfo.getNodeType())) {
                // 先检查当前任务
                Task currentTask = currentTaskMap.get(flowNode.getId());
                if (currentTask != null) {
                    nodeInfo.setTaskId(currentTask.getId());
                    if (currentTask.getAssignee() != null) {
                        List<String> assigneeIds = Collections.singletonList(currentTask.getAssignee());
                        nodeInfo.setAssigneeIds(assigneeIds);
                        // 获取用户详情
                        fillUserInfo(nodeInfo, assigneeIds, userInfoCache);
                    }
                    // 获取候选人信息
                    if (currentTask.getAssignee() == null) {
                        // 任务未签收，获取候选人 - 使用 TaskService 查询
                        List<org.flowable.identitylink.api.IdentityLink> identityLinks = taskService.getIdentityLinksForTask(currentTask.getId());
                        if (identityLinks != null && !identityLinks.isEmpty()) {
                            List<String> candidateUsers = identityLinks.stream()
                                .filter(link -> link.getUserId() != null && "candidate".equals(link.getType()))
                                .map(org.flowable.identitylink.api.IdentityLink::getUserId)
                                .distinct()
                                .collect(Collectors.toList());
                            if (!candidateUsers.isEmpty()) {
                                nodeInfo.setCandidateUserIds(candidateUsers);
                                fillUserInfo(nodeInfo, candidateUsers, userInfoCache);
                            }
                        }
                    }
                } else {
                    // 检查历史任务
                    HistoricTaskInstance historicTask = taskMap.get(flowNode.getId());
                    if (historicTask != null) {
                        if (historicTask.getAssignee() != null) {
                            List<String> assigneeIds = Collections.singletonList(historicTask.getAssignee());
                            nodeInfo.setAssigneeIds(assigneeIds);
                            // 获取用户详情
                            fillUserInfo(nodeInfo, assigneeIds, userInfoCache);
                        }
                        nodeInfo.setStartTime(historicTask.getCreateTime());
                        nodeInfo.setEndTime(historicTask.getEndTime());
                        if (historicTask.getDurationInMillis() != null) {
                            nodeInfo.setDuration(historicTask.getDurationInMillis());
                        }
                    }
                }
            }
            
            nodeList.add(nodeInfo);
        }
        
        return nodeList;
    }
    
    /**
     * 填充用户信息（姓名、组织）
     */
    private void fillUserInfo(ProcessNodeInfo nodeInfo, List<String> userIds,
                              Map<String, Map<String, Object>> userInfoCache) {
        if (flowOrgIntegrationService == null || userIds == null || userIds.isEmpty()) {
            return;
        }

        List<String> missingIds = userIds.stream()
                .filter(id -> id != null && !id.isBlank() && !userInfoCache.containsKey(id))
                .distinct()
                .toList();
        if (!missingIds.isEmpty()) {
            Map<String, Map<String, Object>> loaded = flowOrgIntegrationService.getUserInfoBatch(missingIds);
            if (loaded != null) {
                userInfoCache.putAll(loaded);
            }
            missingIds.forEach(id -> userInfoCache.putIfAbsent(id, Collections.emptyMap()));
        }

        List<String> names = new ArrayList<>();
        List<String> orgs = new ArrayList<>();
        List<Map<String, Object>> details = new ArrayList<>();

        for (String userId : userIds) {
            try {
                Map<String, Object> userInfo = userInfoCache.get(userId);
                if (userInfo != null) {
                    // 获取用户名
                    String name = (String) userInfo.get("name");
                    if (name == null) {
                        name = (String) userInfo.get("nickname");
                    }
                    if (name == null) {
                        name = (String) userInfo.get("username");
                    }
                    if (name != null) {
                        names.add(name);
                    } else {
                        names.add(userId); // 如果没有名字，显示ID
                    }
                    
                    // 获取组织名称
                    String orgName = (String) userInfo.get("deptName");
                    if (orgName == null) {
                        orgName = (String) userInfo.get("orgName");
                    }
                    if (orgName != null) {
                        orgs.add(orgName);
                    }
                    
                    // 添加详情
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("userId", userId);
                    detail.put("name", name != null ? name : userId);
                    detail.put("orgName", orgName);
                    details.add(detail);
                } else {
                    names.add(userId);
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败: userId={}", userId, e);
                names.add(userId);
            }
        }
        
        nodeInfo.setAssigneeNames(names);
        nodeInfo.setAssigneeOrgs(orgs);
        nodeInfo.setAssigneeDetails(details);
    }

}

