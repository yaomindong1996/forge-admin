package com.mdframe.forge.plugin.system.service.plugin;

import com.mdframe.forge.plugin.system.dto.PluginArtifactMetadataDTO;
import com.mdframe.forge.plugin.system.dto.PluginArtifactRegisterDTO;
import com.mdframe.forge.plugin.system.entity.SysPluginArtifactRegistration;
import com.mdframe.forge.plugin.system.mapper.SysPluginArtifactMapper;
import com.mdframe.forge.plugin.system.mapper.SysPluginBuildMapper;
import com.mdframe.forge.plugin.system.mapper.SysPluginTaskMapper;
import com.mdframe.forge.plugin.system.mapper.SysPluginTaskReviewMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/** 登记控制面只保存人工元数据，不执行上传代码、远程请求或部署。 */
@Service
@RequiredArgsConstructor
public class PluginArtifactRegistrationService {
    private final SysPluginBuildMapper builds;
    private final SysPluginTaskMapper tasks;
    private final SysPluginTaskReviewMapper reviews;
    private final SysPluginArtifactMapper artifacts;
    private final PluginReleaseApprovalValidator approvals;
    private final PluginArtifactCodec codec;

    @Transactional(rollbackFor = Exception.class)
    public void register(String id, PluginArtifactRegisterDTO command, PluginTaskActor actor) {
        var metadata = codec.parse(command.getMetadataJson());
        codec.normalize(command, metadata);
        require(id.equals(metadata.getTaskId()), "元数据不属于当前任务");
        // 与finish/close统一build→task顺序；锁内重新验证，不复用CLI的旧核验响应。
        require(builds.lockSummary(actor.tenantId(), id) != null, "任务不存在或尚未构建");
        require(tasks.lockTask(actor.tenantId(), id) != null, "任务不存在");
        String digest = codec.digest(command);
        var previous = artifacts.selectRequest(actor.tenantId(), id, actor.userId(), command.getRequestId());
        if (previous != null) {
            require(digest.equals(previous.getCommandSha256()), "同一登记请求不能变更内容");
            return;
        }
        approvals.validate(reviews.selectApprovalSnapshot(actor.tenantId(), id, metadata.getReviewId()),
                metadata.approvalCommand());
        require(artifacts.selectTask(actor.tenantId(), id).isEmpty(), "该任务已有候选登记，请刷新记录，不能覆盖");
        artifacts.insert(audit(metadata, command, actor, digest));
    }

    private SysPluginArtifactRegistration audit(PluginArtifactMetadataDTO metadata,
                                               PluginArtifactRegisterDTO command, PluginTaskActor actor,
                                               String digest) {
        var value = new SysPluginArtifactRegistration();
        value.setId(UUID.randomUUID().toString());
        value.setTaskId(metadata.getTaskId());
        value.setReviewId(metadata.getReviewId());
        value.setRequestId(command.getRequestId());
        value.setCommandSha256(digest);
        value.setExpectedRevision(metadata.getRevision());
        value.setReleaseId(metadata.getReleaseId());
        value.setManifestSha256(metadata.getManifestSha256());
        value.setRepositoryId(metadata.getRepositoryId());
        value.setServerResultSha256(metadata.getServerResultSha256());
        value.setMetadataJson(command.getMetadataJson());
        value.setLocalVerified(command.getLocalVerified());
        value.setNotDeployed(command.getNotDeployed());
        value.setNote(command.getNote());
        value.setTenantId(actor.tenantId());
        value.setCreateBy(actor.userId());
        value.setUpdateBy(actor.userId());
        value.setCreateDept(actor.deptId());
        value.setCreateTime(LocalDateTime.now());
        value.setUpdateTime(value.getCreateTime());
        value.setDelFlag(0);
        return value;
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(409, message);
        }
    }
}
