package com.mdframe.forge.plugin.ai.provider.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiProviderMapper extends BaseMapper<AiProvider> {

    Page<AiProvider> selectProviderPage(
            Page<AiProvider> page,
            @Param("providerName") String providerName,
            @Param("providerType") String providerType,
            @Param("status") String status
    );

    AiProvider selectDefaultProvider();
}
