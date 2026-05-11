package com.mdframe.forge.plugin.external.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.external.dto.ExternalSystemQuery;
import com.mdframe.forge.plugin.external.entity.ExternalSystem;
import com.mdframe.forge.plugin.external.mapper.ExternalSystemMapper;
import com.mdframe.forge.plugin.external.service.ExternalSystemService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalSystemServiceImpl extends ServiceImpl<ExternalSystemMapper, ExternalSystem>
        implements ExternalSystemService {

    private final ExternalSystemMapper systemMapper;

    @Override
    public IPage<ExternalSystem> page(ExternalSystemQuery query) {
        query.setTenantId(SessionHelper.getTenantId());
        Page<ExternalSystem> page = new Page<>(query.getPageNum(), query.getPageSize());
        return systemMapper.selectSystemPage(page, query);
    }

    @Override
    public List<ExternalSystem> listAll() {
        Long tenantId = SessionHelper.getTenantId();
        return systemMapper.selectSystemList(tenantId);
    }

    @Override
    public ExternalSystem getByCode(String systemCode) {
        Long tenantId = SessionHelper.getTenantId();
        return systemMapper.selectSystemByCode(systemCode, tenantId);
    }
}
