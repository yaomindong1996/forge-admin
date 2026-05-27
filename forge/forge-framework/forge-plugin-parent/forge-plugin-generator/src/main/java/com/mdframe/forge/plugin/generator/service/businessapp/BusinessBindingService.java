package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessBindingBatchSaveDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessBindingDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessBindingQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessBindingVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 业务应用平台能力挂接服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessBindingService extends ServiceImpl<BusinessBindingMapper, AiBusinessBinding> {

    private static final Set<String> TARGET_TYPES = Set.of("SUITE", "OBJECT", "APP");
    private static final Set<String> BINDING_TYPES = Set.of(
            "FLOW", "APPROVAL", "REPORT", "PERMISSION", "MESSAGE",
            "TRIGGER", "IMPORT", "EXPORT", "MOBILE", "INTEGRATION"
    );

    private final BusinessSuiteService suiteService;
    private final BusinessObjectService objectService;
    private final BusinessAppService appService;

    public List<BusinessBindingVO> list(BusinessBindingQueryDTO query) {
        return baseMapper.selectBindingList(resolveTenantId(), normalizeQuery(query));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(BusinessBindingDTO dto) {
        if (dto == null) {
            throw new BusinessException("能力挂接不能为空");
        }
        AiBusinessBinding binding = new AiBusinessBinding();
        copyDtoToEntity(dto, binding, true);
        save(binding);
        return binding.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(BusinessBindingDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("能力挂接ID不能为空");
        }
        AiBusinessBinding binding = requireBinding(dto.getId());
        copyDtoToEntity(dto, binding, false);
        updateById(binding);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiBusinessBinding binding = requireBinding(id);
        removeById(binding.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchSave(BusinessBindingBatchSaveDTO dto) {
        if (dto == null) {
            throw new BusinessException("批量挂接参数不能为空");
        }
        String targetType = normalizeTargetType(dto.getTargetType());
        String targetCode = StringUtils.trimToNull(dto.getTargetCode());
        if (StringUtils.isBlank(targetCode)) {
            throw new BusinessException("挂接目标编码不能为空");
        }
        validateTarget(targetType, dto.getTargetId(), targetCode);
        if (dto.getBindings() == null || dto.getBindings().isEmpty()) {
            throw new BusinessException("批量挂接能力不能为空");
        }
        List<String> bindingTypes = dto.getBindings().stream()
                .filter(item -> item != null && StringUtils.isNotBlank(item.getBindingType()))
                .map(item -> item.getBindingType().toUpperCase())
                .distinct()
                .toList();
        if (bindingTypes.isEmpty()) {
            throw new BusinessException("批量挂接能力类型不能为空");
        }
        baseMapper.deleteByBatchScope(resolveTenantId(), targetType, targetCode, bindingTypes);
        for (BusinessBindingDTO item : dto.getBindings()) {
            if (item == null) {
                continue;
            }
            item.setTargetType(targetType);
            item.setTargetId(dto.getTargetId());
            item.setTargetCode(targetCode);
            AiBusinessBinding binding = new AiBusinessBinding();
            copyDtoToEntity(item, binding, true);
            save(binding);
        }
    }

    private void copyDtoToEntity(BusinessBindingDTO dto, AiBusinessBinding binding, boolean create) {
        String targetType = normalizeTargetType(dto.getTargetType());
        String targetCode = StringUtils.trimToNull(dto.getTargetCode());
        String bindingType = normalizeBindingType(dto.getBindingType());
        String bindingKey = StringUtils.trimToNull(dto.getBindingKey());
        String bindingName = StringUtils.trimToNull(dto.getBindingName());
        if (StringUtils.isBlank(targetCode)) {
            throw new BusinessException("挂接目标编码不能为空");
        }
        if (StringUtils.isBlank(bindingKey)) {
            throw new BusinessException("能力业务键不能为空");
        }
        if (StringUtils.isBlank(bindingName)) {
            throw new BusinessException("挂接名称不能为空");
        }
        validateTarget(targetType, dto.getTargetId(), targetCode);
        Long excludeId = create ? null : binding.getId();
        if (baseMapper.countByScope(resolveTenantId(), targetType, targetCode, bindingType, bindingKey, excludeId) > 0) {
            throw new BusinessException("能力挂接已存在: " + bindingName);
        }
        binding.setTenantId(resolveTenantId());
        binding.setTargetType(targetType);
        binding.setTargetId(dto.getTargetId());
        binding.setTargetCode(targetCode);
        binding.setBindingType(bindingType);
        binding.setBindingKey(bindingKey);
        binding.setBindingName(bindingName);
        binding.setBindingConfig(StringUtils.trimToNull(dto.getBindingConfig()));
        binding.setDescription(StringUtils.trimToNull(dto.getDescription()));
        binding.setStatus(normalizeStatus(dto.getStatus()));
        binding.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
    }

    private void validateTarget(String targetType, Long targetId, String targetCode) {
        if ("SUITE".equals(targetType)) {
            suiteService.requireByCode(targetCode);
            return;
        }
        if ("OBJECT".equals(targetType)) {
            if (targetId == null) {
                return;
            }
            objectService.requireEntity(targetId);
            return;
        }
        if ("APP".equals(targetType)) {
            if (targetId == null) {
                return;
            }
            appService.requireEntity(targetId);
        }
    }

    private AiBusinessBinding requireBinding(Long id) {
        if (id == null) {
            throw new BusinessException("能力挂接ID不能为空");
        }
        AiBusinessBinding binding = baseMapper.selectBindingById(resolveTenantId(), id);
        if (binding == null) {
            throw new BusinessException("能力挂接不存在");
        }
        return binding;
    }

    private BusinessBindingQueryDTO normalizeQuery(BusinessBindingQueryDTO query) {
        BusinessBindingQueryDTO result = query == null ? new BusinessBindingQueryDTO() : query;
        result.setTargetType(StringUtils.trimToNull(result.getTargetType()));
        result.setTargetCode(StringUtils.trimToNull(result.getTargetCode()));
        result.setBindingType(StringUtils.trimToNull(result.getBindingType()));
        return result;
    }

    private String normalizeTargetType(String targetType) {
        String value = StringUtils.defaultIfBlank(targetType, "OBJECT").toUpperCase();
        if (!TARGET_TYPES.contains(value)) {
            throw new BusinessException("挂接目标类型不正确");
        }
        return value;
    }

    private String normalizeBindingType(String bindingType) {
        String value = StringUtils.trimToEmpty(bindingType).toUpperCase();
        if (!BINDING_TYPES.contains(value)) {
            throw new BusinessException("能力挂接类型不正确");
        }
        return value;
    }

    private Integer normalizeStatus(Integer status) {
        int value = status == null ? 1 : status;
        if (value != 0 && value != 1) {
            throw new BusinessException("状态值不正确");
        }
        return value;
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}
