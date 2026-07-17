package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiCodeRule;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRuleDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CodeRuleMapper extends BaseMapper<AiCodeRule> {

    Page<AiCodeRule> selectRulePage(Page<AiCodeRule> page,
                                    @Param("tenantId") Long tenantId,
                                    @Param("ruleCode") String ruleCode,
                                    @Param("ruleName") String ruleName,
                                    @Param("scene") String scene,
                                    @Param("category") String category,
                                    @Param("status") Integer status);

    Page<CodeRuleDetailVO> selectDetailPage(Page<CodeRuleDetailVO> page,
                                            @Param("tenantId") Long tenantId,
                                            @Param("ruleCode") String ruleCode,
                                            @Param("ruleName") String ruleName,
                                            @Param("category") String category,
                                            @Param("status") Integer status);

    List<AiCodeRule> selectEnabledList(@Param("tenantId") Long tenantId,
                                       @Param("scene") String scene,
                                       @Param("sourceObjectCode") String sourceObjectCode,
                                       @Param("selectableOnly") Boolean selectableOnly);

    AiCodeRule selectByRuleCode(@Param("tenantId") Long tenantId,
                                @Param("ruleCode") String ruleCode);

    AiCodeRule selectByRuleId(@Param("tenantId") Long tenantId,
                              @Param("id") Long id);

    int countRuleCodeHistory(@Param("tenantId") Long tenantId,
                             @Param("ruleCode") String ruleCode,
                             @Param("excludeId") Long excludeId);
}
