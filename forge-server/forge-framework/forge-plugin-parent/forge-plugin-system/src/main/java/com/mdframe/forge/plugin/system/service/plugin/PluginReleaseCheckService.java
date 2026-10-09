package com.mdframe.forge.plugin.system.service.plugin;

import com.mdframe.forge.plugin.system.config.PluginWorkerIdentity;
import com.mdframe.forge.plugin.system.dto.PluginReleaseCheckDTO;
import com.mdframe.forge.plugin.system.mapper.SysPluginTaskReviewMapper;
import com.mdframe.forge.plugin.system.vo.PluginReleaseCheckVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PluginReleaseCheckService {
    private final SysPluginTaskReviewMapper reviews;
    private final PluginReleaseApprovalValidator validator;

    public PluginReleaseCheckVO check(String id, PluginReleaseCheckDTO command, PluginWorkerIdentity worker) {
        // 单条JOIN为同一读取快照；该只读结果不能用作后续登记/部署的永久授权。
        var snapshot = reviews.selectApprovalSnapshot(worker.tenantId(), id, command.getReviewId());
        if (snapshot == null || !Objects.equals(snapshot.getWorkerId(), worker.workerId())) {
            throw new BusinessException(409, "当前任务审批不可核验");
        }
        validator.validate(snapshot, command);
        return new PluginReleaseCheckVO(1, command.getCheckId(), id, snapshot.getRevision(), snapshot.getReviewId(),
                worker.workerId(), snapshot.getResultSha256(), command.getManifestSha256(),
                Instant.now().truncatedTo(ChronoUnit.MILLIS).toString(), true, false);
    }
}
