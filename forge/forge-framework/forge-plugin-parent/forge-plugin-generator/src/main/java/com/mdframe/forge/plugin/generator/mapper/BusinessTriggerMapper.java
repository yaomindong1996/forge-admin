package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessTrigger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessTriggerMapper extends BaseMapper<AiBusinessTrigger> {

    Page<AiBusinessTrigger> selectTriggerPage(Page<AiBusinessTrigger> page,
                                              @Param("tenantId") Long tenantId,
                                              @Param("objectCode") String objectCode,
                                              @Param("scenarioType") String scenarioType);

    List<AiBusinessTrigger> selectActiveByObjectAndEvent(@Param("tenantId") Long tenantId,
                                                         @Param("objectCode") String objectCode,
                                                         @Param("eventType") String eventType);
}
