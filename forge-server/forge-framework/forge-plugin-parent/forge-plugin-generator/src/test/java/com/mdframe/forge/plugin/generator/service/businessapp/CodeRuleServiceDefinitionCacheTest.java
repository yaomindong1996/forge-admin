package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiCodeRule;
import com.mdframe.forge.plugin.generator.domain.entity.AiCodeRuleSegment;
import com.mdframe.forge.plugin.generator.manager.coderule.CodeRuleDefinition;
import com.mdframe.forge.plugin.generator.manager.coderule.CodeRuleEngine;
import com.mdframe.forge.plugin.generator.manager.coderule.LegacyCodeRuleParser;
import com.mdframe.forge.plugin.generator.mapper.CodeRuleSegmentMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodeRuleServiceDefinitionCacheTest {

    @Test
    void shouldCacheByVersionAndReturnIndependentSegmentCopies() {
        CodeRuleSegmentMapper segmentMapper = mock(CodeRuleSegmentMapper.class);
        CodeRuleService service = new CodeRuleService(
                segmentMapper,
                mock(CodeRuleEngine.class),
                mock(LegacyCodeRuleParser.class));
        AiCodeRule rule = rule(1);
        when(segmentMapper.selectByRuleId(1L, 100L)).thenReturn(List.of(segment("PO-")));

        CodeRuleDefinition first = service.loadDefinition(rule, 1L);
        first.getSegments().get(0).setSegmentValue("MUTATED");
        CodeRuleDefinition second = service.loadDefinition(rule, 1L);

        assertEquals("PO-", second.getSegments().get(0).getSegmentValue());
        assertNotSame(first.getSegments().get(0), second.getSegments().get(0));
        assertEquals(1, second.getLegacyCompatEnabled());
        verify(segmentMapper, times(1)).selectByRuleId(1L, 100L);

        rule.setVersionNo(2);
        CodeRuleDefinition third = service.loadDefinition(rule, 1L);

        assertEquals("PO-", third.getSegments().get(0).getSegmentValue());
        verify(segmentMapper, times(2)).selectByRuleId(1L, 100L);
    }

    private AiCodeRule rule(int versionNo) {
        AiCodeRule rule = new AiCodeRule();
        rule.setId(100L);
        rule.setTenantId(1L);
        rule.setRuleCode("purchase_no");
        rule.setRuleName("采购单号");
        rule.setVersionNo(versionNo);
        rule.setLegacyCompatEnabled(1);
        return rule;
    }

    private AiCodeRuleSegment segment(String value) {
        AiCodeRuleSegment segment = new AiCodeRuleSegment();
        segment.setSegmentKey("fixed_1");
        segment.setSegmentOrder(1);
        segment.setSegmentType("FIXED");
        segment.setSegmentValue(value);
        segment.setVariableSource("CUSTOM");
        segment.setSegmentLength(value.length());
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
}
