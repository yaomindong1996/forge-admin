package com.mdframe.forge.plugin.external.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.external.dto.ExternalApiQuery;
import com.mdframe.forge.plugin.external.entity.ExternalApi;
import com.mdframe.forge.plugin.external.mapper.ExternalApiMapper;
import com.mdframe.forge.plugin.external.service.ExternalApiService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalApiServiceImpl extends ServiceImpl<ExternalApiMapper, ExternalApi>
        implements ExternalApiService {

    private final ExternalApiMapper apiMapper;

    @Override
    public IPage<ExternalApi> page(ExternalApiQuery query) {
        query.setTenantId(SessionHelper.getTenantId());
        Page<ExternalApi> page = new Page<>(query.getPageNum(), query.getPageSize());
        return apiMapper.selectApiPage(page, query);
    }

    @Override
    public List<ExternalApi> listBySystemId(Long systemId) {
        return apiMapper.selectApisBySystemId(systemId);
    }

    @Override
    public ExternalApi getByCode(String apiCode, Long systemId) {
        return apiMapper.selectApiByCode(apiCode, systemId);
    }

    @Override
    public List<ExternalApi> listWithSystem() {
        return apiMapper.selectApiListWithSystem(SessionHelper.getTenantId());
    }
}