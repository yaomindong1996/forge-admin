package com.mdframe.forge.plugin.system.service.plugin;

import com.mdframe.forge.plugin.system.config.PluginDeliveryProperties;
import com.mdframe.forge.plugin.system.config.PluginWorkerIdentity;
import com.mdframe.forge.plugin.system.dto.*;
import com.mdframe.forge.plugin.system.entity.SysPluginDelivery;
import com.mdframe.forge.plugin.system.enums.PluginDeliveryAction;
import com.mdframe.forge.plugin.system.enums.PluginDeliveryStatus;
import com.mdframe.forge.plugin.system.mapper.SysPluginDeliveryMapper;
import com.mdframe.forge.plugin.system.vo.*;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 控制面只调度审计，不运行客户shell、上传对象或执行迁移。 */
@Service @RequiredArgsConstructor
public class PluginDeliveryService {
    private final SysPluginDeliveryMapper mapper;
    private final PluginDeliveryProperties properties;
    private final PluginDeliveryApproval approvals;
    private final PluginArtifactCodec codec;

    public List<PluginDeliveryTargetVO> targets(PluginTaskActor actor) {
        if (!properties.usable(java.time.Instant.now())
                || !Objects.equals(actor.tenantId(), properties.getWorker().getTenantId())) { return List.of(); }
        var states = mapper.targets(actor.tenantId());
        return properties.getTargets().stream().map(config -> {
            properties.target(config.getId(), actor.tenantId());
            var state = states.stream().filter(row -> config.getId().equals(row.id())).findFirst().orElse(null);
            return new PluginDeliveryTargetVO(config.getId(), config.getName(), config.getRepositoryId(),
                    state == null ? null : state.currentReleaseId(), state == null ? null : state.previousReleaseId(),
                    state == null ? null : state.activeTaskId(), state == null ? null : state.unverifiedReleaseId());
        }).toList();
    }

    public List<PluginDeliveryVO> recent(PluginTaskActor actor) {
        mapper.expire(actor.tenantId(), LocalDateTime.now());
        return mapper.recent(actor.tenantId()).stream().map(PluginDeliveryVO::of).toList();
    }

    public List<PluginDeliveryCandidateVO> candidates(PluginTaskActor actor) {
        return mapper.candidates(actor.tenantId()).stream().map(row -> {
            var value = codec.parse(row.getMetadataJson());
            return new PluginDeliveryCandidateVO(row.getTaskId(), row.getReleaseId(), row.getRepositoryId(),
                    value.getPluginId(), value.getPluginVersion());
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public PluginDeliveryVO create(PluginDeliveryCreateDTO dto, PluginTaskActor actor) {
        var target = properties.target(dto.getTargetId(), actor.tenantId());
        String digest = PackageDigests.sha256(codec.encode(dto).getBytes(StandardCharsets.UTF_8));
        mapper.ensureTarget(actor.tenantId(), UUID.randomUUID().toString(), target.getId());
        var state = mapper.lockTarget(actor.tenantId(), target.getId());
        // 先串行化目标，再做当前读；并发重试不能被旧事务快照误判为新任务。
        var previous = mapper.request(actor.tenantId(), actor.userId(), dto.getRequestId());
        if (previous != null) {
            require(digest.equals(previous.getCommandSha256()), "重复请求内容不同，请刷新核查");
            return PluginDeliveryVO.of(previous);
        }
        var metadata = approvals.validate(actor.tenantId(), dto.getTaskId(), dto.getReleaseId());
        require(target.getRepositoryId().equals(metadata.getRepositoryId()), "候选仓库与目标授权不一致");
        require(state.activeTaskId() == null, "目标已有活动任务，不能并发发布或切换");
        var action = PluginDeliveryAction.parse(dto.getAction());
        checkCreate(dto, action, state, actor.tenantId());
        var row = new SysPluginDelivery();
        row.setId(UUID.randomUUID().toString());
        row.setTenantId(actor.tenantId());
        row.setTargetId(target.getId());
        row.setTaskId(dto.getTaskId());
        row.setReleaseId(dto.getReleaseId());
        row.setPreviousReleaseId(state.currentReleaseId());
        row.setUnverifiedReleaseId(state.unverifiedReleaseId());
        row.setAction(action.getCode());
        row.setStatus(PluginDeliveryStatus.QUEUED.getCode());
        row.setRequestId(dto.getRequestId());
        row.setCommandSha256(digest);
        row.setArtifactManifestSha256(metadata.getResult().getArtifactManifestSha256());
        row.setBackupReference(dto.getBackupReference());
        row.setMigrationsReviewed(dto.getMigrationsReviewed());
        row.setBackwardCompatible(dto.getBackwardCompatible());
        row.setNote(dto.getNote().strip());
        row.setCosVerified(false);
        row.setRuntimeVerified(false);
        row.setCreateBy(actor.userId());
        row.setUpdateBy(actor.userId());
        row.setCreateDept(actor.deptId());
        row.setCreateTime(LocalDateTime.now());
        row.setUpdateTime(row.getCreateTime());
        row.setDelFlag(0);
        mapper.insert(row);
        require(mapper.occupy(actor.tenantId(), target.getId(), row.getId()) == 1, "目标已被其它任务占用");
        return PluginDeliveryVO.of(row);
    }

    private void checkCreate(PluginDeliveryCreateDTO dto, PluginDeliveryAction action,
                             PluginDeliveryTargetVO state, Long tenant) {
        require(dto.getNote().strip().length() >= 10, "请填写不少于10字的操作说明");
        if (action == PluginDeliveryAction.PUBLISH) { return; }
        require(state.unverifiedReleaseId() == null || action == PluginDeliveryAction.RESTORE,
                "目标有未核验版本，只能恢复最后确认版本");
        require(dto.getBackupReference() != null && !dto.getBackupReference().isBlank()
                && Boolean.TRUE.equals(dto.getMigrationsReviewed()), "部署前须确认数据库备份和迁移影响");
        require(mapper.published(tenant, dto.getTargetId(), dto.getReleaseId()) > 0, "制品尚未在此目标完成COS读回核验");
        if (action == PluginDeliveryAction.RESTORE) {
            String expected = state.unverifiedReleaseId() == null
                    ? state.previousReleaseId() : state.currentReleaseId();
            require(Boolean.TRUE.equals(dto.getBackwardCompatible()) && dto.getReleaseId().equals(expected),
                    "恢复须选择目标已确认版本并确认数据库向后兼容");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public PluginDeliveryClaimVO claim(String id, PluginDeliveryLeaseDTO dto, PluginWorkerIdentity identity) {
        var row = required(id, identity.tenantId());
        require(PluginDeliveryStatus.QUEUED.matches(row.getStatus()), "任务不能领取，请先核查状态");
        var metadata = approval(row, identity);
        row.setWorkerId(identity.workerId());
        row.setLeaseHash(hash(dto.getLease()));
        row.setStatus(PluginDeliveryStatus.RUNNING.getCode());
        row.setLeaseExpiresTime(LocalDateTime.now().plusSeconds(90));
        row.setDeadlineTime(LocalDateTime.now().plusMinutes(25));
        mapper.updateById(row);
        return claimView(row, dto, metadata);
    }

    @Transactional(rollbackFor = Exception.class)
    public PluginDeliveryClaimVO authorize(String id, PluginDeliveryLeaseDTO dto, PluginWorkerIdentity identity) {
        var row = leased(id, dto, identity);
        var metadata = approval(row, identity);
        row.setLeaseExpiresTime(LocalDateTime.now().plusSeconds(90));
        mapper.updateById(row);
        return claimView(row, dto, metadata);
    }

    @Transactional(rollbackFor = Exception.class)
    public PluginDeliveryVO finish(String id, PluginDeliveryFinishDTO dto, PluginWorkerIdentity identity) {
        var row = required(id, identity.tenantId());
        require(Objects.equals(row.getWorkerId(), identity.workerId())
                && Objects.equals(row.getLeaseHash(), hash(dto.getLease())), "任务租约不匹配");
        require(row.getArtifactManifestSha256().equals(dto.getArtifactManifestSha256()), "回执产物摘要不匹配");
        if (!PluginDeliveryStatus.RUNNING.matches(row.getStatus())) {
            require(row.getStatus().equals(dto.getStatus())
                    && Objects.equals(row.getCosVerified(), dto.getCosVerified())
                    && Objects.equals(row.getRuntimeVerified(), dto.getRuntimeVerified())
                    && Objects.equals(row.getFailureCode(), dto.getFailureCode()), "任务已结束或结果待核查");
            return PluginDeliveryVO.of(row);
        }
        leased(id, dto, identity);
        if (PluginDeliveryStatus.SUCCEEDED.matches(dto.getStatus())) {
            approval(row, identity);
            require(Boolean.TRUE.equals(dto.getCosVerified()) && dto.getFailureCode() == null, "缺少COS读回核验");
            require(PluginDeliveryAction.PUBLISH.matches(row.getAction())
                    ? !Boolean.TRUE.equals(dto.getRuntimeVerified()) : Boolean.TRUE.equals(dto.getRuntimeVerified()),
                    "发布与运行核验回执不一致");
        } else {
            require(dto.getFailureCode() != null && !Boolean.TRUE.equals(dto.getRuntimeVerified()), "失败回执不完整");
        }
        var status = java.util.Arrays.stream(PluginDeliveryStatus.values())
                .filter(value -> value.matches(dto.getStatus())).findFirst().orElseThrow();
        require(status == PluginDeliveryStatus.SUCCEEDED || status == PluginDeliveryStatus.FAILED
                || status == PluginDeliveryStatus.UNCERTAIN, "回执状态无效");
        row.setStatus(status.getCode());
        row.setCosVerified(dto.getCosVerified());
        row.setRuntimeVerified(dto.getRuntimeVerified());
        row.setFailureCode(dto.getFailureCode());
        mapper.updateById(row);
        // 结果未知继续占用目标；不能自动重试或把旧版本登记当作实际恢复。
        if (!PluginDeliveryStatus.UNCERTAIN.matches(row.getStatus())) {
            require(mapper.complete(identity.tenantId(), row) == 1, "目标占用已变化");
        }
        return PluginDeliveryVO.of(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public PluginDeliveryVO reconcile(String id, PluginDeliveryReconcileDTO dto, PluginTaskActor actor) {
        var row = required(id, actor.tenantId());
        require(Boolean.TRUE.equals(dto.getExecutorStopped()) && dto.getNote().strip().length() >= 10,
                "请确认执行器已停止并填写人工核查说明");
        require(PluginDeliveryStatus.UNCERTAIN.matches(row.getStatus())
                || PluginDeliveryStatus.QUEUED.matches(row.getStatus()), "只有未执行或结果未知任务可以人工关闭");
        row.setStatus(PluginDeliveryStatus.RECONCILED.getCode());
        row.setReconcileNote(dto.getNote().strip());
        row.setReconciledBy(actor.userId());
        row.setReconciledTime(LocalDateTime.now());
        row.setUpdateBy(actor.userId());
        mapper.updateById(row);
        require(mapper.complete(actor.tenantId(), row) == 1, "目标占用已变化");
        return PluginDeliveryVO.of(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public PluginDeliveryRecoveryVO recovery(String id, PluginDeliveryRecoveryDTO dto, PluginWorkerIdentity identity) {
        var row = required(id, identity.tenantId());
        properties.target(row.getTargetId(), identity.tenantId());
        require(properties.getWorker().getId().equals(identity.workerId())
                && (row.getWorkerId() == null || row.getWorkerId().equals(identity.workerId())), "执行器身份不匹配");
        require(PluginDeliveryStatus.RECONCILED.matches(row.getStatus()), "任务尚未人工关闭，不能归档目标锁");
        return new PluginDeliveryRecoveryVO(row.getId(), row.getTargetId(), row.getStatus(), dto.getNonce());
    }

    private PluginArtifactMetadataDTO approval(SysPluginDelivery row, PluginWorkerIdentity identity) {
        var config = properties.target(row.getTargetId(), identity.tenantId());
        require(properties.getWorker().getId().equals(identity.workerId()), "执行器身份已变化");
        var metadata = approvals.validate(identity.tenantId(), row.getTaskId(), row.getReleaseId());
        require(config.getRepositoryId().equals(metadata.getRepositoryId()), "目标仓库授权已变化");
        return metadata;
    }
    private SysPluginDelivery required(String id, Long tenant) {
        var row = mapper.lock(tenant, id);
        require(row != null, "任务不存在或无权访问");
        return row;
    }
    private SysPluginDelivery leased(String id, PluginDeliveryLeaseDTO dto, PluginWorkerIdentity identity) {
        var row = required(id, identity.tenantId());
        require(PluginDeliveryStatus.RUNNING.matches(row.getStatus())
                && Objects.equals(row.getWorkerId(), identity.workerId())
                && Objects.equals(row.getLeaseHash(), hash(dto.getLease()))
                && row.getLeaseExpiresTime().isAfter(LocalDateTime.now())
                && row.getDeadlineTime().isAfter(LocalDateTime.now()), "租约失效，请人工核查最终状态");
        return row;
    }
    private PluginDeliveryClaimVO claimView(SysPluginDelivery row, PluginDeliveryLeaseDTO dto,
                                            PluginArtifactMetadataDTO metadata) {
        return new PluginDeliveryClaimVO(row.getId(), row.getTargetId(), row.getAction(), row.getReleaseId(),
                row.getPreviousReleaseId(), row.getStatus(), dto.getNonce(), dto.getLease(), metadata,
                row.getWorkerId(), row.getUnverifiedReleaseId());
    }
    private String hash(String token) {
        return PackageDigests.sha256(token.getBytes(StandardCharsets.US_ASCII));
    }
    private void require(boolean condition, String message) {
        if (!condition) { throw new BusinessException(409, message); }
    }
}
