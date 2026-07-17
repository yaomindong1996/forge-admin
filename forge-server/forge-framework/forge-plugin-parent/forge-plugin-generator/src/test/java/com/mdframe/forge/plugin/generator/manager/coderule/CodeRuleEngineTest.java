package com.mdframe.forge.plugin.generator.manager.coderule;

import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSegmentDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRuleGenerateVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRulePreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.id.service.ISequenceService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeRuleEngineTest {

    private final CodeRuleRadixCodec codec = new CodeRuleRadixCodec();

    @Test
    void shouldEncodeAllSupportedRadicesAndRejectOverflow() {
        assertEquals("0015", codec.encode(15, "DECIMAL", 4, false));
        assertEquals("000F", codec.encode(15, "HEX", 4, false));
        assertEquals("AAB", codec.encode(1, "ALPHA_UPPER", 3, false));
        assertEquals("aab", codec.encode(1, "ALPHA_LOWER", 3, false));
        assertEquals("001", codec.encode(1, "ALPHANUMERIC", 3, false));
        assertFalse(codec.alphabet("ALPHA_UPPER", true).contains("I"));
        assertFalse(codec.alphabet("ALPHANUMERIC", true).contains("O"));
        assertThrows(BusinessException.class, () -> codec.encode(100, "DECIMAL", 2, false));
    }

    @Test
    void previewShouldRenderFiveSegmentsWithoutTakingSequence() {
        CountingSequenceService sequenceService = new CountingSequenceService(8L);
        CodeRuleEngine engine = engine(sequenceService);
        CodeRuleDefinition definition = definition(List.of(
                segment("fixed", 1, "FIXED", "PO-", 3),
                segment("date", 2, "DATE", "yyyyMMdd", 8),
                segment("tenant", 3, "SYS_VAR", "tenantId", 1),
                segment("warehouse", 4, "VARIABLE", "warehouseCode", 3),
                sequence("seq", 5, 4, "HEX", "DAY", 8L)
        ));

        CodeRulePreviewVO result = engine.preview(
                definition,
                Map.of("warehouseCode", "WH1", "tenantId", "forged"),
                Map.of("tenantId", 1L),
                null
        );

        assertEquals("PO-202607161WH10008", result.getPreviewCode());
        assertEquals(0, sequenceService.calls.get());
        assertTrue(result.getValid());
        assertEquals(5, result.getSegmentPreviews().size());
    }

    @Test
    void generateShouldUseTrustedSystemVariablesAndStableGroupedKey() {
        CountingSequenceService sequenceService = new CountingSequenceService(25L);
        CodeRuleEngine engine = engine(sequenceService);
        CodeRuleSegmentDTO group = segment("warehouse", 1, "VARIABLE", "warehouseCode", 3);
        group.setGroupEnabled(1);
        CodeRuleDefinition definition = definition(List.of(
                group,
                segment("separator", 2, "FIXED", "-", 1),
                segment("user", 3, "SYS_VAR", "userId", 2),
                sequence("seq-stable", 4, 3, "DECIMAL", "MONTH", 25L)
        ));

        CodeRuleGenerateVO first = engine.generate(
                definition,
                Map.of("warehouseCode", "WH1", "userId", "999"),
                Map.of("tenantId", 1L, "userId", 12L)
        );
        String firstKey = sequenceService.lastKey;

        CodeRuleGenerateVO second = engine.generate(
                definition,
                Map.of("warehouseCode", "WH2"),
                Map.of("tenantId", 1L, "userId", 12L)
        );

        assertEquals("WH1-12025", first.getCode());
        assertEquals("202607", first.getPeriod());
        assertEquals("code-rule:1:purchase_no:", sequenceService.lastLegacyKeyPrefix);
        assertEquals("202607", sequenceService.lastLegacyPeriod);
        assertTrue(firstKey.length() <= 100);
        assertNotEquals(first.getGroupKey(), second.getGroupKey());
        assertNotEquals(firstKey, sequenceService.lastKey);
    }

    @Test
    void newRulesShouldUseFiniteCapacityWithoutLegacyLookup() {
        CountingSequenceService sequenceService = new CountingSequenceService(1L);
        CodeRuleDefinition definition = definition(List.of(
                sequence("seq-new", 1, 3, "DECIMAL", "NONE", 1L)
        ));
        definition.setLegacyCompatEnabled(0);

        CodeRuleGenerateVO result = engine(sequenceService).generate(
                definition,
                Map.of(),
                Map.of("tenantId", 1L)
        );

        assertEquals("001", result.getCode());
        assertEquals(1, sequenceService.lastAllocationStep);
        assertEquals(999L, sequenceService.lastMaxValue);
        assertEquals(null, sequenceService.lastLegacyKeyPrefix);
        assertEquals(0, sequenceService.legacyResolutionCalls.get());
    }

    @Test
    void generateShouldWidenOnlyToLegacyAllocatedWatermark() {
        CountingSequenceService legacySequence = new CountingSequenceService(1_001L, 1_001L);
        CodeRuleDefinition legacyDefinition = definition(List.of(
                sequence("sequence_1", 1, 3, "DECIMAL", "HOUR", 1L)
        ));
        CodeRuleEngine legacyEngine = engine(legacySequence);

        CodeRuleGenerateVO legacyResult = legacyEngine.generate(
                legacyDefinition,
                Map.of(),
                Map.of("tenantId", 1L)
        );
        legacyEngine.generate(legacyDefinition, Map.of(), Map.of("tenantId", 1L));

        assertEquals("1001", legacyResult.getCode());
        assertEquals(1, legacySequence.legacyResolutionCalls.get());

        CountingSequenceService exhaustedLegacySequence = new CountingSequenceService(10_000L, 1_001L);
        assertThrows(BusinessException.class, () -> engine(exhaustedLegacySequence).generate(
                legacyDefinition,
                Map.of(),
                Map.of("tenantId", 1L)
        ));

        CountingSequenceService newSequence = new CountingSequenceService(1_000L, 1L);
        CodeRuleDefinition newDefinition = definition(List.of(
                sequence("seq-new", 1, 3, "DECIMAL", "HOUR", 1L)
        ));
        CodeRuleEngine newEngine = engine(newSequence);

        assertThrows(BusinessException.class, () -> newEngine.generate(
                newDefinition,
                Map.of(),
                Map.of("tenantId", 1L)
        ));
        assertThrows(BusinessException.class, () -> newEngine.generate(
                newDefinition,
                Map.of(),
                Map.of("tenantId", 1L)
        ));
        assertEquals(1, newSequence.legacyResolutionCalls.get());
    }

    @Test
    void keyCanonicalizationShouldAvoidDelimiterCollisions() {
        CodeRuleSequenceKeyFactory factory = new CodeRuleSequenceKeyFactory();
        Map<String, String> left = new LinkedHashMap<>();
        left.put("a", "1:2");
        left.put("b", "3");
        Map<String, String> right = new LinkedHashMap<>();
        right.put("a", "1");
        right.put("b", "2:3");

        assertNotEquals(factory.groupHash(left), factory.groupHash(right));
    }

    @Test
    void generateShouldResolveBusinessFieldAliasesWithoutWeakeningSystemVariables() {
        CodeRuleEngine engine = engine(new CountingSequenceService(1L));

        CodeRuleGenerateVO snakeField = engine.generate(
                definition(List.of(segment("warehouse", 1, "VARIABLE", "warehouse_code", 3))),
                Map.of("warehouseCode", "WH1"),
                Map.of("tenantId", 1L)
        );
        assertEquals("WH1", snakeField.getCode());

        CodeRuleGenerateVO camelField = engine.generate(
                definition(List.of(segment("customer", 1, "VARIABLE", "customerCode", 3))),
                Map.of("customer_code", "C01"),
                Map.of("tenantId", 1L)
        );
        assertEquals("C01", camelField.getCode());

        assertThrows(BusinessException.class, () -> engine.generate(
                definition(List.of(segment("org", 1, "SYS_VAR", "orgCode", 3))),
                Map.of(),
                Map.of("tenantId", 1L, "org_code", "ORG")
        ));
    }

    @Test
    void shouldRejectOversizedDefinitionsAndBusinessInputsBeforeTakingSequence() {
        CountingSequenceService sequenceService = new CountingSequenceService(1L);
        CodeRuleEngine engine = engine(sequenceService);
        List<CodeRuleSegmentDTO> tooManySegments = new ArrayList<>();
        for (int index = 0; index < 33; index++) {
            tooManySegments.add(segment("fixed_" + index, index + 1, "FIXED", "A", 1));
        }
        assertThrows(BusinessException.class, () -> engine.generate(
                definition(tooManySegments), Map.of(), Map.of("tenantId", 1L)));

        Map<String, Object> tooManyFields = new LinkedHashMap<>();
        for (int index = 0; index < 257; index++) {
            tooManyFields.put("field" + index, "A");
        }
        assertThrows(BusinessException.class, () -> engine.generate(
                definition(List.of(sequence("seq", 1, 3, "DECIMAL", "NONE", 1L))),
                tooManyFields,
                Map.of("tenantId", 1L)));

        CodeRuleSegmentDTO unboundedVariable = segment(
                "external", 1, "VARIABLE", "externalCode", 1);
        unboundedVariable.setSegmentLength(null);
        assertThrows(BusinessException.class, () -> engine.generate(
                definition(List.of(unboundedVariable)),
                Map.of("externalCode", "X".repeat(97)),
                Map.of("tenantId", 1L)));

        CodeRuleSegmentDTO allowedVariable = segment(
                "external", 1, "VARIABLE", "externalCode", 1);
        allowedVariable.setSegmentLength(null);
        assertThrows(BusinessException.class, () -> engine.generate(
                definition(List.of(
                        allowedVariable,
                        segment("separator", 2, "FIXED", "-", 1),
                        sequence("seq", 3, 3, "DECIMAL", "NONE", 1L))),
                Map.of("externalCode", "X".repeat(96)),
                Map.of("tenantId", 1L)));
        assertEquals(0, sequenceService.calls.get());
    }

    private CodeRuleEngine engine(ISequenceService sequenceService) {
        return new CodeRuleEngine(
                sequenceService,
                codec,
                new CodeRuleSequenceKeyFactory(),
                Clock.fixed(Instant.parse("2026-07-16T08:30:00Z"), ZoneId.of("Asia/Shanghai"))
        );
    }

    private CodeRuleDefinition definition(List<CodeRuleSegmentDTO> segments) {
        CodeRuleDefinition definition = new CodeRuleDefinition();
        definition.setTenantId(1L);
        definition.setRuleId(100L);
        definition.setRuleCode("purchase_no");
        definition.setRuleName("采购单号");
        definition.setLegacyCompatEnabled(1);
        definition.setSegments(new ArrayList<>(segments));
        return definition;
    }

    private CodeRuleSegmentDTO segment(String key, int order, String type, String value, int length) {
        CodeRuleSegmentDTO segment = new CodeRuleSegmentDTO();
        segment.setSegmentKey(key);
        segment.setSegmentOrder(order);
        segment.setSegmentType(type);
        segment.setSegmentValue(value);
        segment.setSegmentLength(length);
        segment.setPadEnabled(0);
        segment.setPadDirection("LEFT");
        segment.setGroupEnabled(0);
        segment.setIncludeInCode(1);
        segment.setResetEnabled(0);
        segment.setResetPolicy("NONE");
        segment.setStartValue(1L);
        segment.setExcludeAmbiguous(0);
        return segment;
    }

    private CodeRuleSegmentDTO sequence(String key,
                                        int order,
                                        int length,
                                        String radix,
                                        String resetPolicy,
                                        long startValue) {
        CodeRuleSegmentDTO segment = segment(key, order, "SEQ", null, length);
        segment.setPadEnabled(1);
        segment.setPadChar("0");
        segment.setRadixType(radix);
        segment.setResetEnabled("NONE".equals(resetPolicy) ? 0 : 1);
        segment.setResetPolicy(resetPolicy);
        segment.setStartValue(startValue);
        return segment;
    }

    private static final class CountingSequenceService implements ISequenceService {

        private final long value;
        private final long legacyStartValue;
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicInteger legacyResolutionCalls = new AtomicInteger();
        private String lastKey;
        private String lastLegacyKeyPrefix;
        private String lastLegacyPeriod;
        private int lastAllocationStep;
        private long lastMaxValue;

        private CountingSequenceService(long value) {
            this(value, 1L);
        }

        private CountingSequenceService(long value, long legacyStartValue) {
            this.value = value;
            this.legacyStartValue = legacyStartValue;
        }

        @Override
        public long nextId(String bizKey) {
            return nextId(bizKey, 1L);
        }

        @Override
        public long nextId(String bizKey, long startValue) {
            calls.incrementAndGet();
            lastKey = bizKey;
            return value;
        }

        @Override
        public long nextId(String bizKey,
                           long startValue,
                           String legacyKeyPrefix,
                           String legacyPeriod) {
            lastLegacyKeyPrefix = legacyKeyPrefix;
            lastLegacyPeriod = legacyPeriod;
            return nextId(bizKey, startValue);
        }

        @Override
        public long nextId(String bizKey,
                           long startValue,
                           String legacyKeyPrefix,
                           String legacyPeriod,
                           int allocationStep,
                           long maxValue) {
            lastLegacyKeyPrefix = legacyKeyPrefix;
            lastLegacyPeriod = legacyPeriod;
            lastAllocationStep = allocationStep;
            lastMaxValue = maxValue;
            return nextId(bizKey, startValue);
        }

        @Override
        public long resolveLegacyStartValue(long startValue,
                                            String legacyKeyPrefix,
                                            String legacyPeriod) {
            legacyResolutionCalls.incrementAndGet();
            return legacyStartValue;
        }

        @Override
        public long[] nextBatch(String bizKey, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String nextFormatted(String bizKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] nextFormattedBatch(String bizKey, int size) {
            throw new UnsupportedOperationException();
        }
    }
}
