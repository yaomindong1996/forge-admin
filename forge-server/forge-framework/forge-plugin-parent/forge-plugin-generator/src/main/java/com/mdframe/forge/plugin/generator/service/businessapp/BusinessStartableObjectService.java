package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.mapper.BusinessStartableObjectMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessStartableObjectVO;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 移动端发起审批目录。只返回元数据，新建与提交仍由各自接口做权限校验。
 */
@Service
@RequiredArgsConstructor
public class BusinessStartableObjectService {

    private final BusinessStartableObjectMapper startableObjectMapper;

    public List<BusinessStartableObjectVO> listStartableObjects() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null) {
            return List.of();
        }
        return startableObjectMapper.selectStartableObjects(tenantId);
    }
}
