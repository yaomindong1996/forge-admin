package com.mdframe.forge.plugin.ai.invocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.invocation.domain.AiModelInvocationLog;
import com.mdframe.forge.plugin.ai.invocation.dto.AiInvocationPageQuery;
import com.mdframe.forge.plugin.ai.invocation.vo.AiInvocationLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper public interface AiModelInvocationLogMapper extends BaseMapper<AiModelInvocationLog> {
    Page<AiInvocationLogVO> selectInvocationPage(Page<AiInvocationLogVO> page, @Param("q") AiInvocationPageQuery query);
    Map<String,Object> selectSummary(@Param("q") AiInvocationPageQuery query);
    List<Long> selectLatencies(@Param("q") AiInvocationPageQuery query);
    int deleteBefore(@Param("cutoff") LocalDateTime cutoff);
}
