package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowOverdueReminderRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程任务逾期提醒记录 Mapper。
 */
@Mapper
public interface FlowOverdueReminderRecordMapper extends BaseMapper<FlowOverdueReminderRecord> {

    FlowOverdueReminderRecord selectLatestByTaskId(@Param("tenantId") Long tenantId,
                                                   @Param("taskId") String taskId);

    Integer countDistinctReminderKeysByTaskId(@Param("tenantId") Long tenantId,
                                              @Param("taskId") String taskId);

    FlowOverdueReminderRecord selectByUniqueKey(@Param("tenantId") Long tenantId,
                                                @Param("reminderKey") String reminderKey,
                                                @Param("channel") String channel);

    int claimRetry(@Param("tenantId") Long tenantId,
                   @Param("reminderKey") String reminderKey,
                   @Param("channel") String channel,
                   @Param("now") LocalDateTime now);

    List<FlowOverdueReminderRecord> selectByTaskId(@Param("taskId") String taskId);
}
