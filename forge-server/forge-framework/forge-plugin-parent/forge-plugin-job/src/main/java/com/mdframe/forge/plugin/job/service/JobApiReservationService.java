package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.entity.SysJobApiIdempotency;
import com.mdframe.forge.plugin.job.mapper.SysJobApiIdempotencyMapper;
import com.mdframe.forge.plugin.job.model.JobApiPrincipal;
import com.mdframe.forge.plugin.job.model.JobApiTriggerTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobApiReservationService {

    private final SysJobApiIdempotencyMapper idempotencyMapper;
    private final JobExecutionLifecycleService lifecycleService;
    private final JobProperties jobProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Reservation reserve(
            JobApiPrincipal principal,
            JobApiTriggerTarget target,
            String keyHash,
            LocalDateTime now) {
        SysJobApiIdempotency existing = idempotencyMapper.selectEffective(
                principal.tenantId(), principal.tokenId(), target.getId(), keyHash, now);
        if (existing != null) {
            return new Reservation(existing.getExecutionId(), true);
        }

        idempotencyMapper.expireMatching(
                principal.tenantId(), principal.tokenId(), target.getId(), keyHash, now);
        Long executionId = lifecycleService.reserveOpenApi(target, now);

        SysJobApiIdempotency reservation = new SysJobApiIdempotency();
        reservation.setTenantId(principal.tenantId());
        reservation.setTokenId(principal.tokenId());
        reservation.setJobConfigId(target.getId());
        reservation.setIdempotencyKeyHash(keyHash);
        reservation.setExecutionId(executionId);
        reservation.setExpiresAt(now.plus(jobProperties.getOpenApi().validatedIdempotencyTtl()));
        reservation.setDelFlag(0);
        if (idempotencyMapper.insertReservation(reservation) <= 0) {
            throw new IllegalStateException("创建开放API幂等记录失败");
        }
        return new Reservation(executionId, false);
    }

    public SysJobApiIdempotency findEffective(
            JobApiPrincipal principal,
            JobApiTriggerTarget target,
            String keyHash,
            LocalDateTime now) {
        return idempotencyMapper.selectEffective(
                principal.tenantId(), principal.tokenId(), target.getId(), keyHash, now);
    }

    public record Reservation(Long executionId, boolean reused) {
    }
}
