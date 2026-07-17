package com.mdframe.forge.starter.id.generator;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mdframe.forge.starter.id.entity.SysIdSequence;
import com.mdframe.forge.starter.id.mapper.SysIdSequenceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 基于数据库号段的序列生成器。
 *
 * <p>数据库负责跨实例分配不重叠号段，进程内按业务键串行切换当前号段。
 * 号段预分配允许出现空洞，但不会重复或在段边界跳过仍未消费的当前号段。</p>
 */
@Slf4j
@Component
public class SegmentSequenceGenerator {

    private static final int DEFAULT_STEP = 1000;
    private static final int MAX_ALLOCATE_RETRIES = 8;
    private static final int MAX_BIZ_KEY_LENGTH = 100;
    static final int MAX_SEGMENT_CACHE_SIZE = 10_000;
    private static final int SEGMENT_CACHE_EXPIRE_MINUTES = 60;

    private final SysIdSequenceMapper sequenceMapper;
    private final TransactionTemplate allocationTransaction;

    private final Cache<String, SegmentHolder> segmentCache = Caffeine.newBuilder()
            .maximumSize(MAX_SEGMENT_CACHE_SIZE)
            .expireAfterAccess(SEGMENT_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .build();

    public SegmentSequenceGenerator(SysIdSequenceMapper sequenceMapper,
                                    PlatformTransactionManager transactionManager) {
        this.sequenceMapper = sequenceMapper;
        this.allocationTransaction = new TransactionTemplate(transactionManager);
        this.allocationTransaction.setName("forge-sequence-segment-allocation");
        this.allocationTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.allocationTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    public long nextId(String bizKey) {
        return nextId(bizKey, 1L);
    }

    /**
     * 获取下一个序列值。startValue 只在业务键首次写入数据库时生效。
     */
    public long nextId(String bizKey, long startValue) {
        return nextId(bizKey, startValue, null, null);
    }

    /**
     * 获取下一个序列值。新 key 第一次初始化时可读取旧编码规则的已分配水位。
     */
    public long nextId(String bizKey,
                       long startValue,
                       String legacyKeyPrefix,
                       String legacyPeriod) {
        return nextId(
                bizKey, startValue, legacyKeyPrefix, legacyPeriod, DEFAULT_STEP, Long.MAX_VALUE);
    }

    /**
     * 获取有限容量序列值。数据库号段会按剩余容量裁剪，禁止预分配越过 maxValue。
     */
    public long nextId(String bizKey,
                       long startValue,
                       String legacyKeyPrefix,
                       String legacyPeriod,
                       int allocationStep,
                       long maxValue) {
        validateArguments(bizKey, startValue, allocationStep, maxValue);
        SegmentHolder holder = segmentCache.get(bizKey, ignored -> new SegmentHolder());
        return holder.next(
                () -> loadSegment(
                        bizKey, startValue, legacyKeyPrefix, legacyPeriod, allocationStep, maxValue),
                maxValue);
    }

    long estimatedSegmentCacheSize() {
        return segmentCache.estimatedSize();
    }

    void cleanUpSegmentCache() {
        segmentCache.cleanUp();
    }

    public long[] nextBatch(String bizKey, int size) {
        if (size < 0) {
            throw new IllegalArgumentException("批量序列数量不能小于0");
        }
        long[] ids = new long[size];
        for (int i = 0; i < size; i++) {
            ids[i] = nextId(bizKey);
        }
        return ids;
    }

    private Segment loadSegment(String bizKey,
                                long startValue,
                                String legacyKeyPrefix,
                                String legacyPeriod,
                                int allocationStep,
                                long maxValue) {
        Segment segment = allocationTransaction.execute(status ->
                allocateSegment(
                        bizKey, startValue, legacyKeyPrefix, legacyPeriod, allocationStep, maxValue));
        if (segment == null) {
            throw new IllegalStateException("分配ID号段事务未返回结果: " + bizKey);
        }
        return segment;
    }

    private Segment allocateSegment(String bizKey,
                                    long startValue,
                                    String legacyKeyPrefix,
                                    String legacyPeriod,
                                    int allocationStep,
                                    long maxValue) {
        SysIdSequence sequence = sequenceMapper.selectById(bizKey);
        if (sequence == null) {
            sequence = initSequence(
                    bizKey,
                    resolveLegacyStartValue(startValue, legacyKeyPrefix, legacyPeriod),
                    allocationStep);
        }

        for (int attempt = 1; attempt <= MAX_ALLOCATE_RETRIES; attempt++) {
            int version = sequence.getVersion() == null ? 0 : sequence.getVersion();
            long currentMaxId = requireMaxId(sequence, bizKey);
            if (currentMaxId >= maxValue) {
                throw new IllegalStateException("序列已达到配置容量上限: " + bizKey);
            }
            long remaining = maxValue - currentMaxId;
            int step = (int) Math.min(normalizeStep(sequence.getStep(), allocationStep), remaining);
            long end = safeAdd(currentMaxId, step, bizKey);
            int updated = sequenceMapper.allocateSegment(bizKey, step, version);
            if (updated > 0) {
                long start = currentMaxId + 1;
                log.debug("成功分配号段: bizKey={}, range=[{}, {}]", bizKey, start, end);
                return new Segment(start, end);
            }

            sequence = sequenceMapper.selectById(bizKey);
            if (sequence == null) {
                sequence = initSequence(
                        bizKey,
                        resolveLegacyStartValue(startValue, legacyKeyPrefix, legacyPeriod),
                        allocationStep);
            }
            log.debug("分配号段版本冲突，准备第{}次重试: {}", attempt + 1, bizKey);
        }
        throw new IllegalStateException("分配ID号段失败，请稍后重试: " + bizKey);
    }

    public long resolveLegacyStartValue(long startValue,
                                        String legacyKeyPrefix,
                                        String legacyPeriod) {
        if (startValue < 0) {
            throw new IllegalArgumentException("序列起始值不能小于0");
        }
        if (!StringUtils.hasText(legacyKeyPrefix) || !StringUtils.hasText(legacyPeriod)) {
            return startValue;
        }
        Long legacyMaxId = sequenceMapper.selectLegacyMaxId(
                escapeLikePrefixPattern(legacyKeyPrefix), legacyPeriod);
        if (legacyMaxId == null) {
            return startValue;
        }
        long legacyNextValue = safeAdd(legacyMaxId, 1, legacyKeyPrefix);
        return Math.max(startValue, legacyNextValue);
    }

    private String escapeLikePrefixPattern(String value) {
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_") + "%";
    }

    private long requireMaxId(SysIdSequence sequence, String bizKey) {
        if (sequence.getMaxId() == null) {
            throw new IllegalStateException("序列最大水位不能为空: " + bizKey);
        }
        return sequence.getMaxId();
    }

    private long safeAdd(long value, long increment, String bizKey) {
        try {
            return Math.addExact(value, increment);
        } catch (ArithmeticException e) {
            throw new IllegalStateException("序列水位已超过long范围: " + bizKey, e);
        }
    }

    private SysIdSequence initSequence(String bizKey, long startValue, int allocationStep) {
        SysIdSequence existing = sequenceMapper.selectById(bizKey);
        if (existing != null) {
            return existing;
        }

        SysIdSequence sequence = new SysIdSequence();
        sequence.setBizKey(bizKey);
        sequence.setMaxId(startValue - 1);
        sequence.setStep(normalizeRequestedStep(allocationStep));
        sequence.setVersion(0);
        sequence.setResetPolicy("NONE");
        sequence.setSeqLength(8);

        try {
            int inserted = sequenceMapper.insert(sequence);
            if (inserted > 0) {
                log.debug("初始化序列配置: bizKey={}, startValue={}", bizKey, startValue);
                return sequence;
            }
        } catch (DuplicateKeyException e) {
            log.debug("序列配置已被其它实例初始化: {}", bizKey);
        }

        existing = sequenceMapper.selectById(bizKey);
        if (existing == null) {
            throw new IllegalStateException("初始化序列配置失败: " + bizKey);
        }
        return existing;
    }

    private int normalizeStep(Integer storedStep, int requestedStep) {
        int normalizedStored = storedStep == null || storedStep <= 0 ? DEFAULT_STEP : storedStep;
        return Math.min(normalizedStored, normalizeRequestedStep(requestedStep));
    }

    private int normalizeRequestedStep(int requestedStep) {
        return Math.min(Math.max(requestedStep, 1), DEFAULT_STEP);
    }

    private void validateArguments(String bizKey,
                                   long startValue,
                                   int allocationStep,
                                   long maxValue) {
        if (!StringUtils.hasText(bizKey)) {
            throw new IllegalArgumentException("业务序列键不能为空");
        }
        if (bizKey.length() > MAX_BIZ_KEY_LENGTH) {
            throw new IllegalArgumentException("业务序列键长度不能超过" + MAX_BIZ_KEY_LENGTH);
        }
        if (startValue < 0) {
            throw new IllegalArgumentException("序列起始值不能小于0");
        }
        if (allocationStep <= 0) {
            throw new IllegalArgumentException("号段分配步长必须大于0");
        }
        if (maxValue < startValue) {
            throw new IllegalArgumentException("序列容量上限不能小于起始值");
        }
    }

    private static final class SegmentHolder {
        private volatile Segment current;

        private long next(Supplier<Segment> segmentLoader, long maxValue) {
            Segment snapshot = current;
            Long value = snapshot == null ? null : snapshot.tryNext(maxValue);
            if (value != null) {
                return value;
            }
            synchronized (this) {
                snapshot = current;
                value = snapshot == null ? null : snapshot.tryNext(maxValue);
                if (value != null) {
                    return value;
                }
                current = segmentLoader.get();
                value = current.tryNext(maxValue);
                if (value == null) {
                    throw new IllegalStateException("ID号段未包含可用序列值");
                }
                return value;
            }
        }
    }

    private static final class Segment {

        private final long end;
        private final AtomicLong cursor;

        private Segment(long start, long end) {
            this.end = end;
            this.cursor = new AtomicLong(start - 1);
        }

        private Long tryNext(long maxValue) {
            long upperBound = Math.min(end, maxValue);
            while (true) {
                long currentValue = cursor.get();
                if (currentValue >= upperBound) {
                    return null;
                }
                long nextValue = currentValue + 1;
                if (cursor.compareAndSet(currentValue, nextValue)) {
                    return nextValue;
                }
            }
        }
    }
}
