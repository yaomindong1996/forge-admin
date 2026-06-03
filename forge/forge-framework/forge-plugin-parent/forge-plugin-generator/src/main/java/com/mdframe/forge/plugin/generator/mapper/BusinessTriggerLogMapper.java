package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessTriggerLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface BusinessTriggerLogMapper extends BaseMapper<AiBusinessTriggerLog> {

    Page<AiBusinessTriggerLog> selectTriggerLogPage(Page<AiBusinessTriggerLog> page,
                                                    @Param("tenantId") Long tenantId,
                                                    @Param("triggerId") Long triggerId);

    Long countSuccessOrTodoSince(@Param("tenantId") Long tenantId,
                                 @Param("triggerId") Long triggerId,
                                 @Param("recordId") String recordId,
                                 @Param("eventType") String eventType,
                                 @Param("sinceTime") LocalDateTime sinceTime);
}
