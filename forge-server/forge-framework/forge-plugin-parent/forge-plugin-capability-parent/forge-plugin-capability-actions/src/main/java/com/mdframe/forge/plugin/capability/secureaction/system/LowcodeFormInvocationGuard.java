package com.mdframe.forge.plugin.capability.secureaction.system;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mdframe.forge.plugin.capability.secureaction.mapper.LowcodeFormReceiptMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.orm.support.DuplicateKeyActions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** 只管理平台已有调用回执，不负责业务表写入或运行库建表。 */
@Slf4j
public class LowcodeFormInvocationGuard {
    private final LowcodeFormReceiptMapper receipts;
    private final TransactionTemplate transaction;
    private final TransactionTemplate withoutTransaction;

    public LowcodeFormInvocationGuard(LowcodeFormReceiptMapper receipts, PlatformTransactionManager manager) {
        this.receipts = receipts;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        withoutTransaction = new TransactionTemplate(manager);
        withoutTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    }

    public Map<String, Object> execute(Request request, boolean local, Supplier<Map<String, Object>> create) {
        if (local) {
            return transaction.execute(status -> {
                Claim claim = reserve(request);
                if (!claim.recordId().isBlank()) { return response(claim.recordId(), true); }
                // 既有空占位只能由原请求继续；不能在数据源配置变化后重放跨库未知请求。
                requireOwner(claim);
                return complete(request, create.get());
            });
        }
        // 跨库仍走普通新增接口逻辑；占位先独立提交，结果不确定时不自动再次调用。
        Claim claim = Objects.requireNonNull(transaction.execute(status -> reserve(request)));
        if (!claim.recordId().isBlank()) { return response(claim.recordId(), true); }
        requireOwner(claim);
        try {
            Map<String, Object> created = withoutTransaction.execute(status -> create.get());
            return transaction.execute(status -> complete(request, created));
        } catch (RuntimeException exception) {
            // 只记录定位标识与异常类型，禁止将表单正文或可能含 SQL 参数的异常写入日志。
            log.warn("开放填报结果待核对，capabilityId={}, keyHash={}, exceptionType={}",
                    request.capability(), request.keyHash(), exception.getClass().getSimpleName());
            throw uncertain();
        }
    }

    private Claim reserve(Request request) {
        long candidate = IdWorker.getId();
        DuplicateKeyActions.insertIgnoreConflict(() -> receipts.reserve(candidate, request.tenant(), request.client(),
                request.capability(), request.keyHash(), request.digest(), request.actor(), request.org()));
        Map<String, Object> receipt = receipts.lock(request.tenant(), request.client(), request.capability(), request.keyHash());
        if (receipt == null || !request.digest().equals(receipt.get("requestDigest"))) {
            throw new BusinessException(409, "IDEMPOTENCY_CONFLICT");
        }
        // MySQL 的 affectedRows 会受到 useAffectedRows 影响，不能用 INSERT 返回值判断占位所有权。
        return new Claim(Objects.toString(receipt.get("recordId"), ""),
                String.valueOf(candidate).equals(Objects.toString(receipt.get("id"), "")));
    }

    private void requireOwner(Claim claim) {
        if (!claim.owner()) { throw uncertain(); }
    }

    private Map<String, Object> complete(Request request, Map<String, Object> created) {
        String recordId = created == null ? "" : Objects.toString(created.get("id"), "");
        if (recordId.isBlank() || receipts.complete(request.tenant(), request.client(), request.capability(),
                request.keyHash(), recordId, request.actor()) != 1) {
            throw new BusinessException("表单新增未返回有效记录或调用回执保存失败");
        }
        return response(recordId, false);
    }

    private Map<String, Object> response(String recordId, boolean hit) {
        return Map.of("recordId", recordId, "idempotentHit", hit);
    }

    private BusinessException uncertain() {
        return new BusinessException(409, "FORM_SUBMISSION_UNCERTAIN：请求正在处理或上次新增结果尚未确认，请先在应用中核对记录；不要自动更换幂等键重复提交");
    }

    public record Request(Long tenant, Long client, Long capability, String keyHash, String digest, Long actor, Long org) { }
    private record Claim(String recordId, boolean owner) { }
}
