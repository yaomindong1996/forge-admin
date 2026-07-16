package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCodeRuleSegment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CodeRuleSegmentMapper extends BaseMapper<AiCodeRuleSegment> {

    List<AiCodeRuleSegment> selectByRuleId(@Param("tenantId") Long tenantId,
                                           @Param("ruleId") Long ruleId);

    int logicalDeleteByRuleId(@Param("tenantId") Long tenantId,
                              @Param("ruleId") Long ruleId,
                              @Param("updateBy") Long updateBy);

    int insertBatch(@Param("segments") List<AiCodeRuleSegment> segments);
}
