package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.flow.dto.VersionCompareDTO;
import com.mdframe.forge.starter.flow.dto.VersionRevertDTO;
import com.mdframe.forge.starter.flow.dto.VersionCleanupDTO;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.enums.FlowModelStatus;
import com.mdframe.forge.starter.flow.entity.FlowModelVersion;
import com.mdframe.forge.starter.flow.helper.BpmnXmlUtils;
import com.mdframe.forge.starter.flow.mapper.FlowModelMapper;
import com.mdframe.forge.starter.flow.mapper.FlowModelVersionMapper;
import com.mdframe.forge.starter.flow.service.FlowModelVersionService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.vo.VersionCompareVO;
import com.mdframe.forge.starter.flow.vo.VersionDetailVO;
import com.mdframe.forge.starter.flow.vo.VersionRevertVO;
import com.mdframe.forge.starter.flow.vo.VersionCleanupVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowModelVersionServiceImpl extends ServiceImpl<FlowModelVersionMapper, FlowModelVersion> implements FlowModelVersionService {

    private final FlowModelMapper flowModelMapper;

    @Autowired(required = false)
    private RepositoryService repositoryService;

    @Autowired(required = false)
    private RuntimeService runtimeService;

    @Override
    public IPage<FlowModelVersion> pageVersionList(Page<FlowModelVersion> page, String modelId) {
        return baseMapper.pageByVersion(page, modelId, requireTenantId());
    }

    @Override
    public VersionDetailVO getVersionDetail(String versionId) {
        FlowModelVersion version = baseMapper.getVersionDetail(versionId, requireTenantId());
        if (version == null) {
            throw new RuntimeException("版本不存在");
        }

        VersionDetailVO vo = new VersionDetailVO();
        vo.setId(version.getId());
        vo.setModelId(version.getModelId());
        vo.setVersion(version.getVersion());
        vo.setVersionName(version.getVersionName());
        vo.setVersionTag(version.getVersionTag());
        vo.setBpmnXml(version.getBpmnXml());
        vo.setFormJson(version.getFormJson());
        vo.setChangeDescription(version.getChangeDescription());
        vo.setDeploymentId(version.getDeploymentId());
        vo.setProcessDefinitionId(version.getProcessDefinitionId());
        vo.setPublishBy(version.getPublishBy());
        vo.setPublishTime(version.getPublishTime() != null ? version.getPublishTime().toString() : null);

        return vo;
    }

    @Override
    public VersionCompareVO compareVersions(VersionCompareDTO dto) {
        Long tenantId = requireTenantId();
        FlowModelVersion version1 = baseMapper.getVersionByModelAndVersion(dto.getModelId(), dto.getVersion1(), tenantId);
        FlowModelVersion version2 = baseMapper.getVersionByModelAndVersion(dto.getModelId(), dto.getVersion2(), tenantId);

        if (version1 == null || version2 == null) {
            throw new RuntimeException("版本不存在");
        }

        VersionCompareVO vo = new VersionCompareVO();
        Map<String, XmlNodeInfo> nodes1 = extractNodes(version1.getBpmnXml());
        Map<String, XmlNodeInfo> nodes2 = extractNodes(version2.getBpmnXml());
        Map<String, XmlFlowInfo> flows1 = extractFlows(version1.getBpmnXml());
        Map<String, XmlFlowInfo> flows2 = extractFlows(version2.getBpmnXml());

        vo.setAddedNodes(new ArrayList<>());
        vo.setModifiedNodes(new ArrayList<>());
        vo.setDeletedNodes(new ArrayList<>());
        vo.setAddedFlows(new ArrayList<>());
        vo.setModifiedFlows(new ArrayList<>());
        vo.setDeletedFlows(new ArrayList<>());

        for (Map.Entry<String, XmlNodeInfo> entry : nodes2.entrySet()) {
            XmlNodeInfo current = entry.getValue();
            XmlNodeInfo previous = nodes1.get(entry.getKey());
            if (previous == null) {
                VersionCompareVO.NodeDiff diff = new VersionCompareVO.NodeDiff();
                diff.setId(current.id);
                diff.setName(current.name);
                vo.getAddedNodes().add(diff);
            }
            else if (!safeEquals(previous.name, current.name)) {
                VersionCompareVO.NodeDiff diff = new VersionCompareVO.NodeDiff();
                diff.setId(current.id);
                diff.setOldName(previous.name);
                diff.setNewName(current.name);
                vo.getModifiedNodes().add(diff);
            }
        }

        for (Map.Entry<String, XmlNodeInfo> entry : nodes1.entrySet()) {
            if (!nodes2.containsKey(entry.getKey())) {
                VersionCompareVO.NodeDiff diff = new VersionCompareVO.NodeDiff();
                diff.setId(entry.getValue().id);
                diff.setName(entry.getValue().name);
                vo.getDeletedNodes().add(diff);
            }
        }

        for (Map.Entry<String, XmlFlowInfo> entry : flows2.entrySet()) {
            XmlFlowInfo current = entry.getValue();
            XmlFlowInfo previous = flows1.get(entry.getKey());
            if (previous == null) {
                VersionCompareVO.FlowDiff diff = new VersionCompareVO.FlowDiff();
                diff.setId(current.id);
                diff.setSource(current.source);
                diff.setTarget(current.target);
                vo.getAddedFlows().add(diff);
            }
            else if (!safeEquals(previous.source, current.source) || !safeEquals(previous.target, current.target)) {
                VersionCompareVO.FlowDiff diff = new VersionCompareVO.FlowDiff();
                diff.setId(current.id);
                diff.setOldSource(previous.source);
                diff.setOldTarget(previous.target);
                diff.setNewSource(current.source);
                diff.setNewTarget(current.target);
                vo.getModifiedFlows().add(diff);
            }
        }

        for (Map.Entry<String, XmlFlowInfo> entry : flows1.entrySet()) {
            if (!flows2.containsKey(entry.getKey())) {
                VersionCompareVO.FlowDiff diff = new VersionCompareVO.FlowDiff();
                diff.setId(entry.getValue().id);
                diff.setSource(entry.getValue().source);
                diff.setTarget(entry.getValue().target);
                vo.getDeletedFlows().add(diff);
            }
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VersionRevertVO revertVersion(VersionRevertDTO dto) {
        Long tenantId = requireTenantId();
        FlowModelVersion targetVersion = baseMapper.getVersionByModelAndVersion(dto.getModelId(), dto.getTargetVersion(), tenantId);

        if (targetVersion == null) {
            throw new RuntimeException("目标版本不存在");
        }
        if (targetVersion.getBpmnXml() == null || targetVersion.getBpmnXml().isBlank()) {
            throw new RuntimeException("目标版本没有 BPMN XML，无法回退");
        }

        FlowModel model = flowModelMapper.selectByIdAndTenant(dto.getModelId(), tenantId);
        if (model == null) {
            throw new RuntimeException("模型不存在");
        }
        if (!tenantId.equals(targetVersion.getTenantId())) {
            throw new RuntimeException("无权操作其他租户的模型版本");
        }
        if (repositoryService == null) {
            throw new RuntimeException("Flowable未初始化");
        }

        Integer newVersion = (model.getVersion() != null ? model.getVersion() : 0) + 1;
        String newVersionId = UUID.randomUUID().toString();
        String newDeploymentId = null;
        String newProcessDefinitionId = null;
        Integer runningInstances = 0;

        if (runtimeService != null && model.getProcessDefinitionId() != null) {
            long count = runtimeService.createProcessInstanceQuery()
                    .processDefinitionId(model.getProcessDefinitionId())
                    .count();
            runningInstances = count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
        }

        String deploymentKey = model.getModelKey() + "_v" + newVersion;
        String deployXml = replaceProcessId(targetVersion.getBpmnXml(), model.getModelKey());
        deployXml = normalizeBpmnXml(deployXml, "版本回退部署");
        Deployment deployment = repositoryService.createDeployment()
                .addString(model.getModelKey() + ".bpmn20.xml", deployXml)
                .name(model.getModelName() + "_v" + newVersion)
                .key(deploymentKey)
                .deploy();

        newDeploymentId = deployment.getId();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(newDeploymentId)
                .singleResult();
        if (processDefinition == null) {
            throw new RuntimeException("版本回退部署失败，未生成流程定义");
        }
        newProcessDefinitionId = processDefinition.getId();
        log.info("版本回退部署成功：deploymentId={}, processDefinitionId={}, newVersion={}", newDeploymentId, newProcessDefinitionId, newVersion);

        FlowModelVersion newVersionRecord = new FlowModelVersion();
        newVersionRecord.setId(newVersionId);
        newVersionRecord.setModelId(dto.getModelId());
        newVersionRecord.setVersion(newVersion);
        newVersionRecord.setVersionName("回退自 v" + dto.getTargetVersion());
        newVersionRecord.setVersionTag("release");
        newVersionRecord.setBpmnXml(deployXml);
        newVersionRecord.setFormJson(targetVersion.getFormJson());
        newVersionRecord.setChangeDescription("回退自 v" + dto.getTargetVersion() + ": " + dto.getChangeDescription());
        newVersionRecord.setDeploymentId(newDeploymentId);
        newVersionRecord.setProcessDefinitionId(newProcessDefinitionId);
        newVersionRecord.setPublishBy(model.getLastUpdateBy());
        newVersionRecord.setPublishTime(LocalDateTime.now());
        newVersionRecord.setTenantId(tenantId);
        newVersionRecord.setCreateTime(LocalDateTime.now());
        newVersionRecord.setDelFlag(0);

        save(newVersionRecord);

        LocalDateTime now = LocalDateTime.now();
        model.setBpmnXml(deployXml);
        model.setFormJson(targetVersion.getFormJson());
        model.setVersion(newVersion);
        model.setDeploymentId(newDeploymentId);
        model.setDeploymentKey(deploymentKey);
        model.setProcessDefinitionId(newProcessDefinitionId);
        model.setStatus(FlowModelStatus.PUBLISHED.getCode());
        model.setDeployTime(now);
        model.setUpdateTime(now);
        flowModelMapper.updateById(model);

        VersionRevertVO vo = new VersionRevertVO();
        vo.setNewVersionId(newVersionId);
        vo.setNewVersion(newVersion);
        vo.setDeploymentId(newDeploymentId);
        vo.setRunningInstances(runningInstances);

        log.info("版本回退成功：modelId={}, targetVersion={}, newVersion={}", dto.getModelId(), dto.getTargetVersion(), newVersion);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVersionTag(String versionId, String versionTag) {
        Long tenantId = requireTenantId();
        FlowModelVersion version = baseMapper.getVersionDetail(versionId, tenantId);
        if (version == null) {
            throw new RuntimeException("版本不存在");
        }

        if ("release".equals(version.getVersionTag())) {
            throw new RuntimeException("已发布版本不可修改标记");
        }

        if (baseMapper.updateVersionTagByIdAndTenant(versionId, tenantId, versionTag) != 1) {
            throw new RuntimeException("版本标记更新失败或已被其他请求处理");
        }
        log.info("版本标记更新成功：versionId={}, versionTag={}", versionId, versionTag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVersion(String versionId) {
        Long tenantId = requireTenantId();
        FlowModelVersion version = baseMapper.getVersionDetail(versionId, tenantId);
        if (version == null) {
            throw new RuntimeException("版本不存在");
        }

        if ("release".equals(version.getVersionTag()) || "deprecated".equals(version.getVersionTag())) {
            throw new RuntimeException("已发布版本和已废弃版本不可删除");
        }

        if (repositoryService != null && version.getProcessDefinitionId() != null) {
            if (runtimeService == null) {
                throw new RuntimeException("运行时服务未初始化，无法确认版本引用");
            }
            long running = runtimeService.createProcessInstanceQuery()
                    .processDefinitionId(version.getProcessDefinitionId()).count();
            if (running > 0) {
                throw new RuntimeException("该版本仍有运行中的流程实例，禁止删除");
            }
        }
        if (baseMapper.logicalDeleteByIdAndTenant(versionId, tenantId) != 1) {
            throw new RuntimeException("版本删除失败或已被其他请求处理");
        }
        log.info("版本删除成功：versionId={}", versionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertVersionOnPublish(FlowModel model, String changeDescription) {
        Long tenantId = requireTenantId();
        if (model.getTenantId() != null && !tenantId.equals(model.getTenantId())) {
            throw new RuntimeException("无权为其他租户的模型创建版本");
        }
        Integer maxVersion = baseMapper.getMaxVersion(model.getId(), tenantId);
        Integer currentVersion = maxVersion != null ? maxVersion + 1 : 1;

        FlowModelVersion versionRecord = new FlowModelVersion();
        versionRecord.setId(UUID.randomUUID().toString());
        versionRecord.setModelId(model.getId());
        versionRecord.setVersion(currentVersion);
        versionRecord.setVersionName("v" + currentVersion);
        versionRecord.setVersionTag("release");
        versionRecord.setBpmnXml(model.getBpmnXml());
        versionRecord.setFormJson(model.getFormJson());
        versionRecord.setChangeDescription(changeDescription != null ? changeDescription : "版本发布");
        versionRecord.setDeploymentId(model.getDeploymentId());
        versionRecord.setProcessDefinitionId(model.getProcessDefinitionId());
        versionRecord.setPublishBy(model.getLastUpdateBy());
        versionRecord.setPublishTime(LocalDateTime.now());
        versionRecord.setTenantId(tenantId);
        versionRecord.setCreateTime(LocalDateTime.now());
        versionRecord.setDelFlag(0);

        save(versionRecord);
        log.info("发布时插入版本历史记录：modelId={}, version={}", model.getId(), currentVersion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VersionCleanupVO cleanupVersions(VersionCleanupDTO dto) {
        Long tenantId = requireTenantId();
        if (dto == null || dto.getModelId() == null || dto.getModelId().isBlank()) {
            throw new IllegalArgumentException("模型ID不能为空");
        }
        int retainLatest = dto.getRetainLatest() == null ? 10 : dto.getRetainLatest();
        if (retainLatest < 1 || retainLatest > 100) {
            throw new IllegalArgumentException("保留版本数必须在1到100之间");
        }
        FlowModel model = flowModelMapper.selectByIdAndTenant(dto.getModelId(), tenantId);
        if (model == null) {
            throw new IllegalArgumentException("模型不存在");
        }
        List<FlowModelVersion> candidates = baseMapper.selectCleanupCandidates(dto.getModelId(), tenantId);
        VersionCleanupVO result = new VersionCleanupVO();
        result.setModelId(dto.getModelId());
        result.setRetainLatest(retainLatest);
        result.setScanned(candidates == null ? 0 : candidates.size());
        int deleted = 0;
        int protectedCount = 0;
        int runningCount = 0;
        int currentCount = 0;
        if (candidates != null) {
            for (int index = 0; index < candidates.size(); index++) {
                FlowModelVersion version = candidates.get(index);
                if (index < retainLatest) {
                    continue;
                }
                if ("release".equals(version.getVersionTag())
                        || "deprecated".equals(version.getVersionTag())) {
                    protectedCount++;
                    continue;
                }
                if (Objects.equals(model.getVersion(), version.getVersion())
                        || Objects.equals(model.getProcessDefinitionId(), version.getProcessDefinitionId())) {
                    currentCount++;
                    continue;
                }
                if (isReferencedByRunningInstance(version)) {
                    runningCount++;
                    continue;
                }
                if (baseMapper.logicalDeleteByIdAndTenant(version.getId(), tenantId) == 1) {
                    deleted++;
                }
            }
        }
        result.setDeleted(deleted);
        result.setSkippedProtected(protectedCount);
        result.setSkippedRunning(runningCount);
        result.setSkippedCurrent(currentCount);
        log.info("清理流程模型历史版本: tenantId={}, modelId={}, retainLatest={}, scanned={}, deleted={}, skippedRunning={}",
                tenantId, dto.getModelId(), retainLatest, result.getScanned(), deleted, runningCount);
        return result;
    }

    private boolean isReferencedByRunningInstance(FlowModelVersion version) {
        if (runtimeService == null || version == null || version.getProcessDefinitionId() == null
                || version.getProcessDefinitionId().isBlank()) {
            return false;
        }
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionId(version.getProcessDefinitionId())
                .count() > 0;
    }

    private Long requireTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("缺少可信租户上下文");
        }
        return tenantId;
    }

    private String extractProcessKey(String bpmnXml) {
        try {
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
        }
        catch (Exception e) {
            log.warn("提取流程Key失败", e);
            return null;
        }
    }

    private String replaceProcessId(String bpmnXml, String modelKey) {
        try {
            String currentProcessId = extractProcessKey(bpmnXml);
            if (currentProcessId == null || currentProcessId.equals(modelKey)) {
                return bpmnXml;
            }

            bpmnXml = bpmnXml.replace(
                    "<bpmn:process id=\"" + currentProcessId + "\"",
                    "<bpmn:process id=\"" + modelKey + "\"");
            bpmnXml = bpmnXml.replace(
                    "<process id=\"" + currentProcessId + "\"",
                    "<process id=\"" + modelKey + "\"");
            bpmnXml = bpmnXml.replace(
                    "bpmnElement=\"" + currentProcessId + "\"",
                    "bpmnElement=\"" + modelKey + "\"");
            return bpmnXml;
        }
        catch (Exception e) {
            log.warn("替换流程ID失败", e);
            return bpmnXml;
        }
    }

    private Map<String, XmlNodeInfo> extractNodes(String bpmnXml) {
        Map<String, XmlNodeInfo> result = new LinkedHashMap<>();
        if (bpmnXml == null || bpmnXml.isBlank()) {
            return result;
        }
        try {
            Document document = parseXml(bpmnXml);
            NodeList allNodes = document.getElementsByTagName("*");
            for (int i = 0; i < allNodes.getLength(); i++) {
                Node node = allNodes.item(i);
                String tagName = localName(node.getNodeName());
                if (shouldSkipTag(tagName)) {
                    continue;
                }
                if (node.getAttributes() == null || node.getAttributes().getNamedItem("id") == null) {
                    continue;
                }
                String id = node.getAttributes().getNamedItem("id").getNodeValue();
                String name = node.getAttributes().getNamedItem("name") != null
                        ? node.getAttributes().getNamedItem("name").getNodeValue()
                        : id;
                result.put(id, new XmlNodeInfo(id, name));
            }
        }
        catch (Exception e) {
            throw new RuntimeException("解析 BPMN XML 失败：" + e.getMessage(), e);
        }
        return result;
    }

    private Map<String, XmlFlowInfo> extractFlows(String bpmnXml) {
        Map<String, XmlFlowInfo> result = new LinkedHashMap<>();
        if (bpmnXml == null || bpmnXml.isBlank()) {
            return result;
        }
        try {
            Document document = parseXml(bpmnXml);
            NodeList allNodes = document.getElementsByTagName("*");
            for (int i = 0; i < allNodes.getLength(); i++) {
                Node node = allNodes.item(i);
                if (!"sequenceFlow".equals(localName(node.getNodeName()))) {
                    continue;
                }
                if (node.getAttributes() == null || node.getAttributes().getNamedItem("id") == null) {
                    continue;
                }
                String id = node.getAttributes().getNamedItem("id").getNodeValue();
                String source = node.getAttributes().getNamedItem("sourceRef") != null
                        ? node.getAttributes().getNamedItem("sourceRef").getNodeValue()
                        : null;
                String target = node.getAttributes().getNamedItem("targetRef") != null
                        ? node.getAttributes().getNamedItem("targetRef").getNodeValue()
                        : null;
                result.put(id, new XmlFlowInfo(id, source, target));
            }
        }
        catch (Exception e) {
            throw new RuntimeException("解析 BPMN 连线失败：" + e.getMessage(), e);
        }
        return result;
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private boolean shouldSkipTag(String tagName) {
        return "definitions".equals(tagName)
                || "process".equals(tagName)
                || "extensionElements".equals(tagName)
                || "documentation".equals(tagName)
                || "sequenceFlow".equals(tagName)
                || "incoming".equals(tagName)
                || "outgoing".equals(tagName)
                || "BPMNDiagram".equals(tagName)
                || "BPMNShape".equals(tagName)
                || "BPMNEdge".equals(tagName);
    }

    private String localName(String tagName) {
        int idx = tagName.indexOf(':');
        return idx >= 0 ? tagName.substring(idx + 1) : tagName;
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
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

    private static class XmlNodeInfo {
        private final String id;
        private final String name;

        private XmlNodeInfo(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static class XmlFlowInfo {
        private final String id;
        private final String source;
        private final String target;

        private XmlFlowInfo(String id, String source, String target) {
            this.id = id;
            this.source = source;
            this.target = target;
        }
    }
}
