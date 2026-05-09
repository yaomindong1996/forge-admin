package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.flow.dto.VersionCompareDTO;
import com.mdframe.forge.starter.flow.dto.VersionRevertDTO;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowModelVersion;
import com.mdframe.forge.starter.flow.mapper.FlowModelMapper;
import com.mdframe.forge.starter.flow.mapper.FlowModelVersionMapper;
import com.mdframe.forge.starter.flow.service.FlowModelVersionService;
import com.mdframe.forge.starter.flow.vo.VersionCompareVO;
import com.mdframe.forge.starter.flow.vo.VersionDetailVO;
import com.mdframe.forge.starter.flow.vo.VersionRevertVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowModelVersionServiceImpl extends ServiceImpl<FlowModelVersionMapper, FlowModelVersion> implements FlowModelVersionService {

    private final FlowModelMapper flowModelMapper;

    @Autowired(required = false)
    private RepositoryService repositoryService;

    @Override
    public IPage<FlowModelVersion> pageVersionList(Page<FlowModelVersion> page, String modelId) {
        return baseMapper.pageByVersion(page, modelId);
    }

    @Override
    public VersionDetailVO getVersionDetail(String versionId) {
        FlowModelVersion version = baseMapper.getVersionDetail(versionId);
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
        FlowModelVersion version1 = getOne(new LambdaQueryWrapper<FlowModelVersion>()
                .eq(FlowModelVersion::getModelId, dto.getModelId())
                .eq(FlowModelVersion::getVersion, dto.getVersion1()));

        FlowModelVersion version2 = getOne(new LambdaQueryWrapper<FlowModelVersion>()
                .eq(FlowModelVersion::getModelId, dto.getModelId())
                .eq(FlowModelVersion::getVersion, dto.getVersion2()));

        if (version1 == null || version2 == null) {
            throw new RuntimeException("版本不存在");
        }

        VersionCompareVO vo = new VersionCompareVO();
        vo.setAddedNodes(new ArrayList<>());
        vo.setModifiedNodes(new ArrayList<>());
        vo.setDeletedNodes(new ArrayList<>());
        vo.setAddedFlows(new ArrayList<>());
        vo.setModifiedFlows(new ArrayList<>());
        vo.setDeletedFlows(new ArrayList<>());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VersionRevertVO revertVersion(VersionRevertDTO dto) {
        FlowModelVersion targetVersion = getOne(new LambdaQueryWrapper<FlowModelVersion>()
                .eq(FlowModelVersion::getModelId, dto.getModelId())
                .eq(FlowModelVersion::getVersion, dto.getTargetVersion()));

        if (targetVersion == null) {
            throw new RuntimeException("目标版本不存在");
        }

        FlowModel model = flowModelMapper.selectById(dto.getModelId());
        if (model == null) {
            throw new RuntimeException("模型不存在");
        }

        Integer newVersion = model.getVersion() + 1;
        String newVersionId = UUID.randomUUID().toString();
        String newDeploymentId = null;

        if (repositoryService != null && targetVersion.getBpmnXml() != null) {
            String deploymentKey = model.getModelKey() + "_v" + newVersion;
            Deployment deployment = repositoryService.createDeployment()
                    .addString(deploymentKey + ".bpmn20.xml", targetVersion.getBpmnXml())
                    .name(model.getModelName() + "_v" + newVersion)
                    .key(deploymentKey)
                    .deploy();

            newDeploymentId = deployment.getId();
            log.info("版本回退部署成功：deploymentId={}, newVersion={}", newDeploymentId, newVersion);
        }

        FlowModelVersion newVersionRecord = new FlowModelVersion();
        newVersionRecord.setId(newVersionId);
        newVersionRecord.setModelId(dto.getModelId());
        newVersionRecord.setVersion(newVersion);
        newVersionRecord.setVersionName("回退自 v" + dto.getTargetVersion());
        newVersionRecord.setVersionTag("release");
        newVersionRecord.setBpmnXml(targetVersion.getBpmnXml());
        newVersionRecord.setFormJson(targetVersion.getFormJson());
        newVersionRecord.setChangeDescription("回退自 v" + dto.getTargetVersion() + ": " + dto.getChangeDescription());
        newVersionRecord.setDeploymentId(newDeploymentId);
        newVersionRecord.setPublishBy(model.getLastUpdateBy());
        newVersionRecord.setPublishTime(LocalDateTime.now());
        newVersionRecord.setTenantId(1L);
        newVersionRecord.setCreateTime(LocalDateTime.now());
        newVersionRecord.setDelFlag(0);

        save(newVersionRecord);

        model.setVersion(newVersion);
        model.setDeploymentId(newDeploymentId);
        model.setUpdateTime(LocalDateTime.now());
        flowModelMapper.updateById(model);

        VersionRevertVO vo = new VersionRevertVO();
        vo.setNewVersionId(newVersionId);
        vo.setNewVersion(newVersion);
        vo.setDeploymentId(newDeploymentId);
        vo.setRunningInstances(0);

        log.info("版本回退成功：modelId={}, targetVersion={}, newVersion={}", dto.getModelId(), dto.getTargetVersion(), newVersion);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVersionTag(String versionId, String versionTag) {
        FlowModelVersion version = getById(versionId);
        if (version == null) {
            throw new RuntimeException("版本不存在");
        }

        if ("release".equals(version.getVersionTag())) {
            throw new RuntimeException("已发布版本不可修改标记");
        }

        version.setVersionTag(versionTag);
        updateById(version);
        log.info("版本标记更新成功：versionId={}, versionTag={}", versionId, versionTag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVersion(String versionId) {
        FlowModelVersion version = getById(versionId);
        if (version == null) {
            throw new RuntimeException("版本不存在");
        }

        if ("release".equals(version.getVersionTag()) || "deprecated".equals(version.getVersionTag())) {
            throw new RuntimeException("已发布版本和已废弃版本不可删除");
        }

        removeById(versionId);
        log.info("版本删除成功：versionId={}", versionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertVersionOnPublish(FlowModel model, String changeDescription) {
        Integer maxVersion = baseMapper.getMaxVersion(model.getId());
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
        versionRecord.setTenantId(1L);
        versionRecord.setCreateTime(LocalDateTime.now());
        versionRecord.setDelFlag(0);

        save(versionRecord);
        log.info("发布时插入版本历史记录：modelId={}, version={}", model.getId(), currentVersion);
    }
}