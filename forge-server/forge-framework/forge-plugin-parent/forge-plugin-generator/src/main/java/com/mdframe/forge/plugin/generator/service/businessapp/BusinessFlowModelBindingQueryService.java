package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowModelBindingsQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessBindingSummaryVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessFlowModelBindingQueryService {
    private final BusinessBindingMapper bindingMapper;

    /** 输入校验由 HTTP DTO 负责；租户始终取已认证会话，不接受客户端传入。 */
    public List<BusinessBindingSummaryVO> list(BusinessFlowModelBindingsQueryDTO query) {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("无法读取当前租户的业务绑定");
        }
        List<String> keys = query.getModelKeys().stream().map(String::trim).distinct().toList();
        return bindingMapper.selectFlowBindingsByModelKeys(tenantId, keys);
    }
}
