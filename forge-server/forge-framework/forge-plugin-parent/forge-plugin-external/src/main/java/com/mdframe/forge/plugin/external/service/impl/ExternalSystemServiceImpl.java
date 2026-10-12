package com.mdframe.forge.plugin.external.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.external.dto.ExternalSystemQuery;
import com.mdframe.forge.plugin.external.entity.ExternalSystem;
import com.mdframe.forge.plugin.external.mapper.ExternalSystemMapper;
import com.mdframe.forge.plugin.external.service.ExternalSystemService;
import com.mdframe.forge.plugin.external.support.ExternalSecretService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalSystemServiceImpl extends ServiceImpl<ExternalSystemMapper, ExternalSystem>
        implements ExternalSystemService {

    private final ExternalSystemMapper systemMapper;
    private final ExternalSecretService secretService;

    @Override
    public IPage<ExternalSystem> page(ExternalSystemQuery query) {
        query.setTenantId(SessionHelper.getTenantId());
        Page<ExternalSystem> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<ExternalSystem> result = systemMapper.selectSystemPage(page, query);
        result.setRecords(result.getRecords().stream().map(secretService::forManagement).toList());
        return result;
    }

    @Override
    public List<ExternalSystem> listAll() {
        Long tenantId = SessionHelper.getTenantId();
        return systemMapper.selectSystemList(tenantId).stream().map(secretService::forManagement).toList();
    }

    @Override
    public ExternalSystem getByCode(String systemCode) {
        Long tenantId = SessionHelper.getTenantId();
        return secretService.forRuntime(systemMapper.selectSystemByCode(systemCode, tenantId));
    }

    @Override
    public ExternalSystem getManagementById(Long id) {
        return secretService.forManagement(systemMapper.selectById(id));
    }

    @Override
    public ExternalSystem getRuntimeById(Long id) {
        return secretService.forRuntime(systemMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveSystem(ExternalSystem entity) {
        ExternalSystem persisted = secretService.prepareForPersistence(entity, null);
        persisted.setTenantId(SessionHelper.getTenantId());
        return systemMapper.insert(persisted) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSystem(ExternalSystem entity) {
        if (entity == null || entity.getId() == null) {
            throw new BusinessException("外部系统ID不能为空");
        }
        ExternalSystem existing = systemMapper.selectById(entity.getId());
        if (existing == null) {
            throw new BusinessException("外部系统不存在");
        }
        ExternalSystem persisted = secretService.prepareForPersistence(entity, existing);
        persisted.setTenantId(SessionHelper.getTenantId());
        return systemMapper.updateById(persisted) == 1;
    }
}
