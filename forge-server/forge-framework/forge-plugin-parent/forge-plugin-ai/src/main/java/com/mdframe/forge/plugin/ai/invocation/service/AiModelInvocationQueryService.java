package com.mdframe.forge.plugin.ai.invocation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.invocation.dto.AiInvocationPageQuery;
import com.mdframe.forge.plugin.ai.invocation.mapper.AiModelInvocationLogMapper;
import com.mdframe.forge.plugin.ai.invocation.vo.AiInvocationLogVO;
import com.mdframe.forge.plugin.ai.invocation.vo.AiInvocationSummaryVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service @RequiredArgsConstructor
public class AiModelInvocationQueryService {
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000L);
    private final AiModelInvocationLogMapper mapper;
    public Page<AiInvocationLogVO> page(AiInvocationPageQuery q) { int n=q.getPageNum()==null?1:q.getPageNum(); int s=q.getPageSize()==null?20:Math.min(q.getPageSize(),200); return mapper.selectInvocationPage(new Page<>(n,s),q); }
    public AiInvocationSummaryVO summarize(AiInvocationPageQuery q) {
        Map<String,Object> m=mapper.selectSummary(q); AiInvocationSummaryVO vo=new AiInvocationSummaryVO();
        vo.setTotalCount(longValue(m,"totalCount")); vo.setSuccessCount(longValue(m,"successCount")); vo.setFailedCount(longValue(m,"failedCount")); vo.setUsageUnavailableCount(longValue(m,"usageUnavailableCount")); vo.setCostUnavailableCount(longValue(m,"costUnavailableCount")); vo.setTotalPromptTokens(longValue(m,"totalPromptTokens")); vo.setTotalCompletionTokens(longValue(m,"totalCompletionTokens"));
        BigDecimal numerator=decimalValue(m,"costNumerator"); try { vo.setEstimatedCostCent(numerator.divide(ONE_MILLION,0,RoundingMode.HALF_UP).longValueExact()); } catch (ArithmeticException e) { throw new BusinessException("成本汇总超出可表示范围"); }
        List<Long> latencies=mapper.selectLatencies(q); vo.setP95LatencyMs(latencies.isEmpty()?null:latencies.get((int)Math.ceil(latencies.size()*0.95D)-1)); return vo;
    }
    private long longValue(Map<String,Object> m,String key){ Object v=find(m,key); return v==null?0L:new BigDecimal(v.toString()).longValue(); }
    private BigDecimal decimalValue(Map<String,Object> m,String key){ Object v=find(m,key); return v==null?BigDecimal.ZERO:new BigDecimal(v.toString()); }
    private Object find(Map<String,Object> m,String key){ if(m==null)return null; if(m.containsKey(key))return m.get(key); String lower=key.toLowerCase(); return m.entrySet().stream().filter(e->e.getKey().replace("_","").equalsIgnoreCase(lower)).map(Map.Entry::getValue).findFirst().orElse(null); }
}
