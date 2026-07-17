package com.mdframe.forge.starter.id.generator;

import com.mdframe.forge.starter.id.entity.SysIdSequence;
import com.mdframe.forge.starter.id.mapper.SysIdSequenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class SegmentSequenceGeneratorTest {

    private final Map<String, SysIdSequence> rows = new ConcurrentHashMap<>();
    private final AtomicReference<Runnable> afterNextAllocation = new AtomicReference<>();
    private String lastLegacyQueryPrefix;

    private SysIdSequenceMapper mapper;
    private RecordingTransactionManager transactionManager;
    private SegmentSequenceGenerator generator;

    @BeforeEach
    void setUp() {
        mapper = (SysIdSequenceMapper) Proxy.newProxyInstance(
                SysIdSequenceMapper.class.getClassLoader(),
                new Class<?>[]{SysIdSequenceMapper.class},
                (proxy, method, arguments) -> invokeMapper(method.getName(), arguments));
        transactionManager = new RecordingTransactionManager();
        generator = new SegmentSequenceGenerator(mapper, transactionManager);
    }

    @Test
    void shouldUseConfiguredStartValueForNewSequence() {
        assertEquals(25L, generator.nextId("code-rule:start", 25L));
        assertEquals(26L, generator.nextId("code-rule:start", 25L));
    }

    @Test
    void shouldRemainContinuousAcrossSegmentBoundary() {
        List<Long> values = new ArrayList<>();
        for (int i = 0; i < 1_200; i++) {
            values.add(generator.nextId("code-rule:boundary", 1L));
        }

        assertEquals(1L, values.get(0));
        assertEquals(1_000L, values.get(999));
        assertEquals(1_001L, values.get(1_000));
        assertEquals(1_200L, values.get(1_199));
    }

    @Test
    void shouldAllocateUniqueValuesConcurrently() throws Exception {
        int threadCount = 8;
        int valuesPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        Set<Long> values = ConcurrentHashMap.newKeySet();

        for (int thread = 0; thread < threadCount; thread++) {
            executor.execute(() -> {
                try {
                    start.await();
                    for (int i = 0; i < valuesPerThread; i++) {
                        values.add(generator.nextId("code-rule:concurrent", 1L));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        executor.shutdownNow();

        assertEquals(threadCount * valuesPerThread, values.size());
        assertEquals(threadCount * valuesPerThread, new HashSet<>(values).size());
    }

    @Test
    void shouldContinueAboveLegacyAllocatedWatermark() {
        SysIdSequence legacy = sequence(
                "code-rule:1:document_daily_no:pw_purchase_order_purchaseNo:20260716",
                5_000L,
                5
        );
        rows.put(legacy.getBizKey(), legacy);

        long value = generator.nextId(
                "cr:1:100:segment:global:20260716",
                1L,
                "code-rule:1:document_daily_no:",
                "20260716"
        );

        assertEquals(5_001L, value);
        assertEquals("code-rule:1:document!_daily!_no:%", lastLegacyQueryPrefix);
    }

    @Test
    void shouldResolveLegacyStartWithoutConsumingSequence() {
        SysIdSequence legacy = sequence(
                "code-rule:1:material_code:legacy_scope:2026071709",
                1_000L,
                1
        );
        rows.put(legacy.getBizKey(), legacy);
        int rowCount = rows.size();

        long value = generator.resolveLegacyStartValue(
                1L,
                "code-rule:1:material_code:",
                "2026071709"
        );

        assertEquals(1_001L, value);
        assertEquals(rowCount, rows.size());
    }

    @Test
    void shouldContinueTimeOnlyAutoRuleFromLegacyAllPeriod() {
        SysIdSequence legacy = sequence(
                "code-rule:1:time_only:legacy_scope:all",
                1_000L,
                1
        );
        rows.put(legacy.getBizKey(), legacy);

        long value = generator.nextId(
                "cr:1:101:segment:global:all",
                1L,
                "code-rule:1:time_only:",
                "all"
        );

        assertEquals(1_001L, value);
    }

    @Test
    void shouldReturnOwnRangeWhenAnotherInstanceAllocatesBeforeReadBack() {
        RecordingTransactionManager competingTransactionManager = new RecordingTransactionManager();
        SegmentSequenceGenerator competingGenerator = new SegmentSequenceGenerator(mapper, competingTransactionManager);
        AtomicReference<Long> competingValue = new AtomicReference<>();
        afterNextAllocation.set(() -> competingValue.set(competingGenerator.nextId("code-rule:multi-instance", 1L)));

        long firstValue = generator.nextId("code-rule:multi-instance", 1L);

        assertEquals(1L, firstValue);
        assertEquals(1_001L, competingValue.get());
    }

    @Test
    void shouldCommitAllocationInRequiresNewTransaction() {
        generator.nextId("code-rule:transaction", 1L);

        assertEquals(List.of(TransactionDefinition.PROPAGATION_REQUIRES_NEW),
                transactionManager.propagationBehaviors);
        assertEquals(List.of(TransactionDefinition.ISOLATION_READ_COMMITTED),
                transactionManager.isolationLevels);
        assertEquals(1, transactionManager.commits);
        assertEquals(0, transactionManager.rollbacks);
    }

    @Test
    void shouldBoundHighCardinalityCacheAndRemainUniqueAfterEviction() {
        long first = generator.nextId("code-rule:evicted", 1L);
        for (int index = 0; index <= SegmentSequenceGenerator.MAX_SEGMENT_CACHE_SIZE; index++) {
            generator.nextId("code-rule:group:" + index, 1L);
        }
        generator.cleanUpSegmentCache();

        assertTrue(generator.estimatedSegmentCacheSize()
                <= SegmentSequenceGenerator.MAX_SEGMENT_CACHE_SIZE);
        assertEquals(1_001L, generator.nextId("code-rule:evicted", 1L));
        assertEquals(1L, first);
    }

    @Test
    void shouldNotLoseFiniteCapacityAfterCacheEviction() {
        long first = generator.nextId(
                "cr:1:finite:segment:global:all", 1L, null, null, 1, 999L);
        for (int index = 0; index <= SegmentSequenceGenerator.MAX_SEGMENT_CACHE_SIZE; index++) {
            generator.nextId("code-rule:finite-group:" + index, 1L);
        }
        generator.cleanUpSegmentCache();

        long second = generator.nextId(
                "cr:1:finite:segment:global:all", 1L, null, null, 1, 999L);

        assertEquals(1L, first);
        assertEquals(2L, second);
    }

    @Test
    void shouldRejectExhaustedCapacityWithoutAdvancingWatermark() {
        String bizKey = "cr:1:exhausted:segment:global:all";
        SysIdSequence exhausted = sequence(bizKey, 999L, 3);
        rows.put(bizKey, exhausted);

        assertThrows(IllegalStateException.class, () -> generator.nextId(
                bizKey, 1L, null, null, 1, 999L));

        assertEquals(999L, rows.get(bizKey).getMaxId());
        assertEquals(3, rows.get(bizKey).getVersion());
    }

    @Test
    void shouldRejectBizKeyLongerThanDatabaseColumn() {
        assertThrows(IllegalArgumentException.class, () -> generator.nextId("x".repeat(101), 1L));
    }

    private SysIdSequence copy(SysIdSequence source) {
        if (source == null) {
            return null;
        }
        SysIdSequence target = new SysIdSequence();
        target.setBizKey(source.getBizKey());
        target.setMaxId(source.getMaxId());
        target.setStep(source.getStep());
        target.setVersion(source.getVersion());
        target.setResetPolicy(source.getResetPolicy());
        target.setSeqLength(source.getSeqLength());
        target.setPrefix(source.getPrefix());
        return target;
    }

    private Object invokeMapper(String methodName, Object[] arguments) {
        return switch (methodName) {
            case "selectById" -> copy(rows.get(String.valueOf(arguments[0])));
            case "insert" -> insert((SysIdSequence) arguments[0]);
            case "allocateSegment" -> allocateSegment(
                    String.valueOf(arguments[0]),
                    ((Number) arguments[1]).intValue(),
                    ((Number) arguments[2]).intValue());
            case "selectLegacyMaxId" -> selectLegacyMaxId(
                    String.valueOf(arguments[0]),
                    String.valueOf(arguments[1]));
            case "toString" -> "InMemorySysIdSequenceMapper";
            case "hashCode" -> System.identityHashCode(this);
            case "equals" -> this == arguments[0];
            default -> throw new UnsupportedOperationException("测试 Mapper 不支持方法: " + methodName);
        };
    }

    private int insert(SysIdSequence source) {
        SysIdSequence sequence = copy(source);
        synchronized (rows) {
            if (rows.containsKey(sequence.getBizKey())) {
                return 0;
            }
            rows.put(sequence.getBizKey(), sequence);
            return 1;
        }
    }

    private int allocateSegment(String bizKey, int step, int expectedVersion) {
        synchronized (rows) {
            SysIdSequence sequence = rows.get(bizKey);
            if (sequence == null || sequence.getVersion() != expectedVersion) {
                return 0;
            }
            sequence.setMaxId(sequence.getMaxId() + step);
            sequence.setVersion(sequence.getVersion() + 1);
            Runnable hook = afterNextAllocation.getAndSet(null);
            if (hook != null) {
                hook.run();
            }
            return 1;
        }
    }

    private Long selectLegacyMaxId(String legacyKeyPrefix, String period) {
        lastLegacyQueryPrefix = legacyKeyPrefix;
        String literalPrefix = unescapeLikePrefixPattern(legacyKeyPrefix);
        return rows.values().stream()
                .filter(item -> item.getBizKey().startsWith(literalPrefix))
                .filter(item -> matchesPeriod(item.getBizKey(), period))
                .map(SysIdSequence::getMaxId)
                .max(Long::compareTo)
                .orElse(null);
    }

    private String unescapeLikePrefixPattern(String value) {
        String pattern = value.endsWith("%")
                ? value.substring(0, value.length() - 1) : value;
        StringBuilder result = new StringBuilder(value.length());
        boolean escaped = false;
        for (int index = 0; index < pattern.length(); index++) {
            char current = pattern.charAt(index);
            if (escaped) {
                result.append(current);
                escaped = false;
            } else if (current == '!') {
                escaped = true;
            } else {
                result.append(current);
            }
        }
        if (escaped) {
            result.append('!');
        }
        return result.toString();
    }

    private boolean matchesPeriod(String bizKey, String period) {
        String legacyPeriod = bizKey.substring(bizKey.lastIndexOf(':') + 1);
        return legacyPeriod.equals(period)
                || (period.length() == 10 && legacyPeriod.startsWith(period));
    }

    private SysIdSequence sequence(String bizKey, long maxId, int version) {
        SysIdSequence sequence = new SysIdSequence();
        sequence.setBizKey(bizKey);
        sequence.setMaxId(maxId);
        sequence.setStep(1_000);
        sequence.setVersion(version);
        sequence.setResetPolicy("NONE");
        sequence.setSeqLength(8);
        return sequence;
    }

    private static final class RecordingTransactionManager implements PlatformTransactionManager {

        private final List<Integer> propagationBehaviors = new ArrayList<>();
        private final List<Integer> isolationLevels = new ArrayList<>();
        private int commits;
        private int rollbacks;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            propagationBehaviors.add(definition.getPropagationBehavior());
            isolationLevels.add(definition.getIsolationLevel());
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            commits++;
        }

        @Override
        public void rollback(TransactionStatus status) {
            rollbacks++;
        }
    }
}
