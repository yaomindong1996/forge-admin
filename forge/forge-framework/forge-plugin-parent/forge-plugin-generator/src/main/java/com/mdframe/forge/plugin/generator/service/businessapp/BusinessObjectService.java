package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectRuntimeInfoVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 业务应用平台业务对象服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessObjectService extends ServiceImpl<BusinessObjectMapper, AiBusinessObject> {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{1,63}$");
    private static final Set<String> OBJECT_TYPES = Set.of("MASTER", "DETAIL", "LOOKUP", "TRANSACTION");

    private final BusinessSuiteService suiteService;
    private final BusinessAppMapper businessAppMapper;

    public Page<BusinessObjectVO> page(Integer pageNum, Integer pageSize, BusinessObjectQueryDTO query) {
        Page<BusinessObjectVO> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return baseMapper.selectObjectPage(page, resolveTenantId(), normalizeQuery(query));
    }

    public List<BusinessObjectVO> list(BusinessObjectQueryDTO query) {
        return baseMapper.selectObjectList(resolveTenantId(), normalizeQuery(query));
    }

    public BusinessObjectVO detail(Long id) {
        BusinessObjectVO vo = baseMapper.selectObjectDetail(resolveTenantId(), id);
        if (vo == null) {
            throw new BusinessException("业务对象不存在");
        }
        return vo;
    }

    public BusinessObjectVO detailByCode(String suiteCode, String objectCode) {
        BusinessObjectVO vo = baseMapper.selectObjectDetailByCode(
                resolveTenantId(), StringUtils.trimToNull(suiteCode), StringUtils.trimToNull(objectCode));
        if (vo == null) {
            throw new BusinessException("业务对象不存在");
        }
        return vo;
    }

    public BusinessObjectRuntimeInfoVO runtimeInfo(Long id) {
        AiBusinessObject object = requireEntity(id);
        AiBusinessApp app = businessAppMapper.selectRuntimeAppByObject(
                resolveTenantId(), object.getSuiteCode(), object.getObjectCode());
        BusinessObjectRuntimeInfoVO vo = new BusinessObjectRuntimeInfoVO();
        vo.setObjectId(object.getId());
        vo.setObjectCode(object.getObjectCode());
        vo.setObjectName(object.getObjectName());
        vo.setObjectStatus(object.getStatus());
        vo.setPermissionGranted(hasPermission("ai:businessApp:open"));
        if (app != null) {
            vo.setAppId(app.getId());
            vo.setAppCode(app.getAppCode());
            vo.setAppName(app.getAppName());
            vo.setConfigKey(app.getConfigKey());
            if (StringUtils.isNotBlank(app.getConfigKey())) {
                vo.setRoutePath("/ai/crud-page/" + app.getConfigKey());
            } else {
                vo.setRoutePath(app.getEntryUrl());
            }
        }
        boolean enabled = Integer.valueOf(1).equals(object.getStatus());
        boolean appEnabled = app != null && Integer.valueOf(1).equals(app.getStatus());
        boolean configured = app != null && (StringUtils.isNotBlank(app.getConfigKey()) || StringUtils.isNotBlank(app.getEntryUrl()));
        vo.setCanOpen(enabled && appEnabled && configured && Boolean.TRUE.equals(vo.getPermissionGranted()));
        if (!enabled) {
            vo.setMessage("业务对象已停用");
        } else if (app == null) {
            vo.setMessage("业务对象尚未配置应用入口");
        } else if (!appEnabled) {
            vo.setMessage("应用入口已停用");
        } else if (!configured) {
            vo.setMessage("应用入口尚未配置运行地址");
        } else if (!Boolean.TRUE.equals(vo.getPermissionGranted())) {
            vo.setMessage("缺少应用入口打开权限");
        } else {
            vo.setMessage("可打开标准业务应用");
        }
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(BusinessObjectDTO dto) {
        if (dto == null) {
            throw new BusinessException("业务对象不能为空");
        }
        AiBusinessObject object = new AiBusinessObject();
        copyDtoToEntity(dto, object, true);
        save(object);
        return object.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(BusinessObjectDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("业务对象ID不能为空");
        }
        AiBusinessObject object = requireEntity(dto.getId());
        copyDtoToEntity(dto, object, false);
        updateById(object);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        AiBusinessObject object = requireEntity(id);
        object.setStatus(normalizeStatus(status));
        updateById(object);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiBusinessObject object = requireEntity(id);
        Long tenantId = resolveTenantId();
        if (baseMapper.countRelationsByObject(tenantId, object.getSuiteCode(), object.getObjectCode()) > 0) {
            throw new BusinessException("该业务对象已存在对象关系，不能删除");
        }
        if (baseMapper.countAppsByObject(tenantId, object.getSuiteCode(), object.getObjectCode()) > 0) {
            throw new BusinessException("该业务对象已关联应用入口，不能删除");
        }
        removeById(object.getId());
    }

    public AiBusinessObject requireEntity(Long id) {
        if (id == null) {
            throw new BusinessException("业务对象ID不能为空");
        }
        AiBusinessObject object = getById(id);
        if (object == null) {
            throw new BusinessException("业务对象不存在");
        }
        return object;
    }

    public AiBusinessObject requireByCode(String suiteCode, String objectCode) {
        String suite = StringUtils.trimToNull(suiteCode);
        String code = StringUtils.trimToNull(objectCode);
        if (StringUtils.isBlank(suite) || StringUtils.isBlank(code)) {
            throw new BusinessException("业务套件编码和对象编码不能为空");
        }
        AiBusinessObject object = baseMapper.selectByObjectCode(resolveTenantId(), suite, code);
        if (object == null) {
            throw new BusinessException("业务对象不存在: " + code);
        }
        return object;
    }

    private void copyDtoToEntity(BusinessObjectDTO dto, AiBusinessObject object, boolean create) {
        String suiteCode = StringUtils.trimToNull(dto.getSuiteCode());
        String objectCode = StringUtils.trimToNull(dto.getObjectCode());
        String objectName = StringUtils.trimToNull(dto.getObjectName());
        String objectType = StringUtils.defaultIfBlank(dto.getObjectType(), "MASTER").toUpperCase();
        suiteService.requireByCode(suiteCode);
        if (StringUtils.isBlank(objectCode) || !CODE_PATTERN.matcher(objectCode).matches()) {
            throw new BusinessException("对象编码格式不正确（字母开头，仅含字母、数字和下划线，2-64字符）");
        }
        if (StringUtils.isBlank(objectName)) {
            throw new BusinessException("对象名称不能为空");
        }
        if (!OBJECT_TYPES.contains(objectType)) {
            throw new BusinessException("对象类型不正确");
        }
        Long excludeId = create ? null : object.getId();
        if (baseMapper.countByObjectCode(resolveTenantId(), suiteCode, objectCode, excludeId) > 0) {
            throw new BusinessException("同一业务套件下对象编码已存在: " + objectCode);
        }
        object.setTenantId(resolveTenantId());
        object.setSuiteCode(suiteCode);
        object.setObjectCode(objectCode);
        object.setObjectName(objectName);
        object.setObjectType(objectType);
        object.setModelId(dto.getModelId());
        object.setModelCode(StringUtils.trimToNull(dto.getModelCode()));
        object.setDisplayField(StringUtils.trimToNull(dto.getDisplayField()));
        object.setIcon(StringUtils.trimToNull(dto.getIcon()));
        object.setDescription(StringUtils.trimToNull(dto.getDescription()));
        object.setStatus(normalizeStatus(dto.getStatus()));
        object.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        object.setOptions(StringUtils.trimToNull(dto.getOptions()));
    }

    private BusinessObjectQueryDTO normalizeQuery(BusinessObjectQueryDTO query) {
        BusinessObjectQueryDTO result = query == null ? new BusinessObjectQueryDTO() : query;
        result.setKeyword(StringUtils.trimToNull(result.getKeyword()));
        result.setSuiteCode(StringUtils.trimToNull(result.getSuiteCode()));
        result.setObjectCode(StringUtils.trimToNull(result.getObjectCode()));
        result.setObjectType(StringUtils.trimToNull(result.getObjectType()));
        result.setModelCode(StringUtils.trimToNull(result.getModelCode()));
        return result;
    }

    private Integer normalizeStatus(Integer status) {
        int value = status == null ? 1 : status;
        if (value != 0 && value != 1) {
            throw new BusinessException("状态值不正确");
        }
        return value;
    }

    private boolean hasPermission(String permission) {
        try {
            return SessionHelper.hasPermission(permission);
        } catch (Exception e) {
            return false;
        }
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
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
