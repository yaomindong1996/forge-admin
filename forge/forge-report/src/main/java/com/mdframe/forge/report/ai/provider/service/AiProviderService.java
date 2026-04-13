package com.mdframe.forge.report.ai.provider.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.report.ai.provider.domain.AiProvider;
import com.mdframe.forge.report.ai.provider.mapper.AiProviderMapper;
import org.springframework.stereotype.Service;

@Service
public class AiProviderService extends ServiceImpl<AiProviderMapper, AiProvider> {

    /**
     * 获取默认供应商
     */
    public AiProvider getDefaultProvider() {
        return getOne(new LambdaQueryWrapper<AiProvider>()
                .eq(AiProvider::getIsDefault, "1")
                .eq(AiProvider::getStatus, "0")
                .last("LIMIT 1"));
    }
}
