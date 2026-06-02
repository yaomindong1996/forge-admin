package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessMessageChannel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 业务消息通道 Mapper。
 */
@Mapper
public interface BusinessMessageChannelMapper extends BaseMapper<AiBusinessMessageChannel> {

    AiBusinessMessageChannel selectByChannelCode(@Param("tenantId") Long tenantId,
                                                 @Param("channelCode") String channelCode);

    List<Long> selectUserIdsByRoleIds(@Param("tenantId") Long tenantId,
                                      @Param("roleIds") List<Long> roleIds);

    List<Long> selectUserIdsByOrgIds(@Param("tenantId") Long tenantId,
                                     @Param("orgIds") List<Long> orgIds);
}
