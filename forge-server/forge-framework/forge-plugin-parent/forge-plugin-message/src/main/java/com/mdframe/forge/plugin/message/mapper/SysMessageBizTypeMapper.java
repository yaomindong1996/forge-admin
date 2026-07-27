package com.mdframe.forge.plugin.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageBizType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysMessageBizTypeMapper extends BaseMapper<SysMessageBizType> {

    Page<SysMessageBizType> selectBizTypePage(Page<SysMessageBizType> page,
        @Param("bizType") String bizType, @Param("bizName") String bizName,
        @Param("enabled") Integer enabled);

    List<SysMessageBizType> selectEnabledBizTypes();
}
