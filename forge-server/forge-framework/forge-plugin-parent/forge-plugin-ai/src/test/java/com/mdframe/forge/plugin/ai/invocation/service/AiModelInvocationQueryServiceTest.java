package com.mdframe.forge.plugin.ai.invocation.service;
import com.mdframe.forge.plugin.ai.invocation.dto.AiInvocationPageQuery;
import com.mdframe.forge.plugin.ai.invocation.mapper.AiModelInvocationLogMapper;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
class AiModelInvocationQueryServiceTest {
    @Test void shouldUseAggregateHalfUpAndNearestRank(){AiModelInvocationLogMapper mapper=mock(AiModelInvocationLogMapper.class);AiInvocationPageQuery q=new AiInvocationPageQuery();when(mapper.selectSummary(q)).thenReturn(Map.of("totalCount",20,"costNumerator",new BigDecimal("1500000")));when(mapper.selectLatencies(q)).thenReturn(java.util.stream.LongStream.rangeClosed(1,20).boxed().toList());var vo=new AiModelInvocationQueryService(mapper).summarize(q);assertEquals(2L,vo.getEstimatedCostCent());assertEquals(19L,vo.getP95LatencyMs());}
}
