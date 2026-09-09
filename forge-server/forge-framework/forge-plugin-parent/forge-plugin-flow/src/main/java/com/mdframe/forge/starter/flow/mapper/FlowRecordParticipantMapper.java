package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowRecordParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface FlowRecordParticipantMapper extends BaseMapper<FlowRecordParticipant> {

    int insertIgnore(FlowRecordParticipant participant);

    List<String> selectBusinessIds(@Param("tenantId") Long tenantId,
                                   @Param("userId") String userId,
                                   @Param("businessType") String businessType);

    List<FlowRecordParticipant> selectRelations(@Param("tenantId") Long tenantId,
                                                @Param("userId") String userId,
                                                @Param("businessType") String businessType,
                                                @Param("businessIds") Collection<String> businessIds);
}
