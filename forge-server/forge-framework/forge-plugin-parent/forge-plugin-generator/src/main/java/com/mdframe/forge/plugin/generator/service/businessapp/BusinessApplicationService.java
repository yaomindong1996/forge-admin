package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationDesignStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 业务应用聚合服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationService extends ServiceImpl<BusinessApplicationMapper, AiBusinessApplication> {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{1,63}$");
    private static final Set<String> SENSITIVE_OPTION_KEYS = Set.of(
            "token", "access_token", "password", "secret", "clientsecret", "client_secret",
            "webhooksecret", "webhook_secret", "apikey", "api_key", "ak", "sk"
    );

    private final BusinessSuiteService suiteService;
    private final BusinessApplicationObjectMapper applicationObjectMapper;
    private final BusinessAppMapper businessAppMapper;

    public Page<BusinessApplicationVO> page(Integer pageNum, Integer pageSize, BusinessApplicationQueryDTO query) {
        Page<BusinessApplicationVO> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return baseMapper.selectApplicationPage(page, resolveTenantId(), normalizeQuery(query));
    }

    public List<BusinessApplicationVO> list(BusinessApplicationQueryDTO query) {
        return baseMapper.selectApplicationList(resolveTenantId(), normalizeQuery(query));
    }

    public BusinessApplicationVO detail(Long id) {
        BusinessApplicationVO application = baseMapper.selectApplicationDetail(resolveTenantId(), id);
        if (application == null) {
            throw new BusinessException("业务应用不存在");
        }
        return application;
    }

    public BusinessApplicationVO publishContext(Long id) {
        BusinessApplicationVO application = baseMapper.selectApplicationPublishContext(resolveTenantId(), id);
        if (application == null) {
            throw new BusinessException("业务应用不存在");
        }
        return application;
    }

    public BusinessApplicationVO detailByCode(String applicationCode) {
        String code = StringUtils.trimToNull(applicationCode);
        BusinessApplicationVO application = baseMapper.selectApplicationDetailByCode(resolveTenantId(), code);
        if (application == null) {
            throw new BusinessException("业务应用不存在");
        }
        return application;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(BusinessApplicationDTO dto) {
        if (dto == null) {
            throw new BusinessException("业务应用不能为空");
        }
        AiBusinessApplication application = new AiBusinessApplication();
        copyDtoToEntity(dto, application, true);
        application.setDesignStatus(BusinessApplicationDesignStatus.DRAFT);
        save(application);
        return application.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(BusinessApplicationDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("业务应用ID不能为空");
        }
        AiBusinessApplication application = requireEntity(dto.getId());
        copyDtoToEntity(dto, application, false);
        updateById(application);
        baseMapper.markChanged(resolveTenantId(), application.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        AiBusinessApplication application = requireEntity(id);
        application.setStatus(normalizeStatus(status));
        updateById(application);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiBusinessApplication application = requireEntity(id);
        Long tenantId = resolveTenantId();
        if (businessAppMapper.countActiveByApplicationId(tenantId, application.getId()) > 0) {
            throw new BusinessException("业务应用存在启用的访问入口，请先停用或迁移访问入口");
        }
        businessAppMapper.detachDisabledByApplicationId(tenantId, application.getId());
        applicationObjectMapper.logicDeleteByApplicationId(tenantId, application.getId());
        removeById(application.getId());
    }

    public AiBusinessApplication requireEntity(Long id) {
        if (id == null) {
            throw new BusinessException("业务应用ID不能为空");
        }
        AiBusinessApplication application = baseMapper.selectEntityById(resolveTenantId(), id);
        if (application == null) {
            throw new BusinessException("业务应用不存在");
        }
        return application;
    }

    public AiBusinessApplication requireByCode(String applicationCode) {
        String code = StringUtils.trimToNull(applicationCode);
        if (code == null) {
            throw new BusinessException("业务应用编码不能为空");
        }
        AiBusinessApplication application = baseMapper.selectEntityByCode(resolveTenantId(), code);
        if (application == null) {
            throw new BusinessException("业务应用不存在: " + code);
        }
        return application;
    }

    public void assertEntryScope(Long applicationId, String suiteCode, String objectCode) {
        if (applicationId == null) {
            return;
        }
        AiBusinessApplication application = requireEntity(applicationId);
        String suite = StringUtils.trimToNull(suiteCode);
        if (!StringUtils.equals(application.getSuiteCode(), suite)) {
            throw new BusinessException("访问入口所属业务域与业务应用不一致");
        }
        String object = StringUtils.trimToNull(objectCode);
        Long tenantId = resolveTenantId();
        if (object != null
                && applicationObjectMapper.countByApplicationId(tenantId, applicationId) > 0
                && applicationObjectMapper.countByApplicationAndObjectCode(
                tenantId, applicationId, suite, object) == 0) {
            throw new BusinessException("访问入口关联的业务对象尚未加入该业务应用");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void markCompositionChanged(Long applicationId) {
        if (applicationId == null) {
            return;
        }
        requireEntity(applicationId);
        baseMapper.markChanged(resolveTenantId(), applicationId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void restoreSnapshotMetadata(Long applicationId, Map<String, Object> snapshot) {
        AiBusinessApplication application = requireEntity(applicationId);
        if (snapshot == null || snapshot.isEmpty()) {
            throw new BusinessException("历史应用元数据快照为空");
        }
        application.setApplicationName(StringUtils.defaultIfBlank(
                text(snapshot.get("applicationName")), application.getApplicationName()));
        application.setIcon(StringUtils.trimToNull(text(snapshot.get("icon"))));
        application.setDescription(StringUtils.trimToNull(text(snapshot.get("description"))));
        application.setStatus(normalizeStatus(integer(snapshot.get("status"), application.getStatus())));
        application.setOptions(normalizeOptions(writeSnapshotOptions(snapshot.get("options"))));
        updateById(application);
    }

    private String writeSnapshotOptions(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return value instanceof String text ? text : JSON.toJSONString(value);
        } catch (Exception e) {
            throw new BusinessException("历史应用配置快照格式不正确");
        }
    }

    private Integer integer(Object value, Integer fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void copyDtoToEntity(BusinessApplicationDTO dto, AiBusinessApplication application, boolean create) {
        String requestedCode = StringUtils.trimToNull(dto.getApplicationCode());
        String applicationCode = create ? requestedCode : application.getApplicationCode();
        if (StringUtils.isBlank(applicationCode) || !CODE_PATTERN.matcher(applicationCode).matches()) {
            throw new BusinessException("应用编码格式不正确（字母开头，仅含字母、数字和下划线，2-64字符）");
        }
        if (!create && requestedCode != null && !StringUtils.equals(requestedCode, application.getApplicationCode())) {
            throw new BusinessException("应用编码创建后不能修改");
        }
        String applicationName = StringUtils.trimToNull(dto.getApplicationName());
        if (applicationName == null) {
            throw new BusinessException("应用名称不能为空");
        }
        String suiteCode = StringUtils.trimToNull(dto.getSuiteCode());
        suiteService.requireByCode(suiteCode);
        if (!create && !StringUtils.equals(application.getSuiteCode(), suiteCode)) {
            assertSuiteMoveAllowed(application.getId());
        }
        Long excludeId = create ? null : application.getId();
        if (baseMapper.countByApplicationCode(resolveTenantId(), applicationCode, excludeId) > 0) {
            throw new BusinessException("应用编码已存在: " + applicationCode);
        }
        String options = normalizeOptions(dto.getOptions());
        application.setTenantId(resolveTenantId());
        application.setApplicationCode(applicationCode);
        application.setApplicationName(applicationName);
        application.setSuiteCode(suiteCode);
        application.setIcon(StringUtils.trimToNull(dto.getIcon()));
        application.setDescription(StringUtils.trimToNull(dto.getDescription()));
        application.setStatus(normalizeStatus(dto.getStatus()));
        application.setOptions(options);
    }

    private void assertSuiteMoveAllowed(Long applicationId) {
        Long tenantId = resolveTenantId();
        if (applicationObjectMapper.countByApplicationId(tenantId, applicationId) > 0
                || businessAppMapper.countByApplicationId(tenantId, applicationId) > 0) {
            throw new BusinessException("业务应用已关联业务对象或访问入口，不能直接移动业务域");
        }
    }

    private String normalizeOptions(String options) {
        String value = StringUtils.trimToNull(options);
        if (value == null) {
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(value);
            if (containsSensitiveKey(json)) {
                throw new BusinessException("应用扩展配置不能保存密码、Token、Secret 或 API Key");
            }
            return json.toJSONString();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("应用扩展配置必须是合法 JSON 对象");
        }
    }

    private boolean containsSensitiveKey(Object value) {
        if (value instanceof JSONObject object) {
            for (String key : object.keySet()) {
                String normalizedKey = key.replace("-", "_").toLowerCase(Locale.ROOT);
                if (SENSITIVE_OPTION_KEYS.contains(normalizedKey) || containsSensitiveKey(object.get(key))) {
                    return true;
                }
            }
        } else if (value instanceof JSONArray array) {
            for (Object item : array) {
                if (containsSensitiveKey(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private BusinessApplicationQueryDTO normalizeQuery(BusinessApplicationQueryDTO query) {
        BusinessApplicationQueryDTO result = query == null ? new BusinessApplicationQueryDTO() : query;
        result.setKeyword(StringUtils.trimToNull(result.getKeyword()));
        result.setApplicationCode(StringUtils.trimToNull(result.getApplicationCode()));
        result.setSuiteCode(StringUtils.trimToNull(result.getSuiteCode()));
        result.setSuiteCodes(normalizeSuiteCodes(result.getSuiteCodes()));
        if ((result.getSuiteCodes() == null || result.getSuiteCodes().isEmpty())
                && result.getSuiteCode() != null) {
            result.setSuiteCodes(suiteService.listSelfAndDescendantCodes(result.getSuiteCode()));
        }
        result.setDesignStatus(StringUtils.trimToNull(result.getDesignStatus()));
        if (result.getStatus() != null) {
            result.setStatus(normalizeStatus(result.getStatus()));
        }
        if (result.getDesignStatus() != null) {
            result.setDesignStatus(result.getDesignStatus().toUpperCase(Locale.ROOT));
            if (!BusinessApplicationDesignStatus.supportedStatuses().contains(result.getDesignStatus())) {
                throw new BusinessException("应用设计状态不正确");
            }
        }
        return result;
    }

    private List<String> normalizeSuiteCodes(List<String> suiteCodes) {
        if (suiteCodes == null || suiteCodes.isEmpty()) {
            return null;
        }
        List<String> normalized = suiteCodes.stream()
                .filter(StringUtils::isNotBlank)
                .flatMap(item -> Arrays.stream(item.split(",")))
                .map(StringUtils::trimToNull)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        return normalized.isEmpty() ? null : normalized;
    }

    private Integer normalizeStatus(Integer status) {
        int value = status == null ? 1 : status;
        if (value != 0 && value != 1) {
            throw new BusinessException("状态值不正确");
        }
        return value;
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
