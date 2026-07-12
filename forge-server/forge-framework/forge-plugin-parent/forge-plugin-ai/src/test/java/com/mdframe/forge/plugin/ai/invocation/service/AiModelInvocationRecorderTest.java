package com.mdframe.forge.plugin.ai.invocation.service;

import com.mdframe.forge.plugin.ai.invocation.*;
import com.mdframe.forge.plugin.ai.invocation.domain.AiModelInvocationLog;
import com.mdframe.forge.plugin.ai.invocation.mapper.AiModelInvocationLogMapper;
import com.mdframe.forge.plugin.ai.routing.constant.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiModelInvocationRecorderTest {
    @Test void usageAndRouteReasonShouldBePersistedWithoutSensitiveFields(){
        AiModelInvocationLogMapper mapper=mock(AiModelInvocationLogMapper.class); AiModelInvocationRecorder recorder=new AiModelInvocationRecorder(mapper);
        recorder.record(new AiInvocationObservation("req",1L,2L,"agent","session",AiInvocationPhase.COMPLETED,true,AiInvocationOutcome.SUCCESS,AiModelRouteSource.POLICY,AiModelRouteReason.POLICY_PRIORITY,3L,4L,5L,"model","adapter",null,null,null,12L,100L,20L,120L,10L,30L));
        ArgumentCaptor<AiModelInvocationLog> captor=ArgumentCaptor.forClass(AiModelInvocationLog.class); verify(mapper).insert(captor.capture()); AiModelInvocationLog log=captor.getValue();
        assertEquals("POLICY",log.getRouteSource()); assertEquals("POLICY_PRIORITY",log.getRouteReason()); assertTrue(log.getUsageAvailable()); assertTrue(log.getCostAvailable());
        assertFalse(java.util.Arrays.stream(AiModelInvocationLog.class.getDeclaredFields()).anyMatch(f -> java.util.Set.of("prompt","response","apiKey","headers","nativeUsage").contains(f.getName())));
    }
    @Test void missingUsageShouldRemainNull(){
        AiModelInvocationLogMapper mapper=mock(AiModelInvocationLogMapper.class);
        new AiModelInvocationRecorder(mapper).record(new AiInvocationObservation(
                "req",1L,null,"agent",null,AiInvocationPhase.COMPLETED,true,AiInvocationOutcome.SUCCESS,
                null,null,null,null,null,null,null,null,null,null,1L,
                null,null,null,null,null));
        ArgumentCaptor<AiModelInvocationLog> c=ArgumentCaptor.forClass(AiModelInvocationLog.class);verify(mapper).insert(c.capture());assertFalse(c.getValue().getUsageAvailable());assertNull(c.getValue().getTotalTokens());
    }
}
