package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppOpenInfoVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 业务应用平台应用入口服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessAppService extends ServiceImpl<BusinessAppMapper, AiBusinessApp> {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{1,63}$");
    private static final Set<String> APP_TYPES = Set.of("BUSINESS", "EMBEDDED", "MOBILE", "INTEGRATION");
    private static final Set<String> ENTRY_MODES = Set.of("RUNTIME", "ROUTE", "IFRAME", "EXTERNAL", "H5", "API");
    private static final Set<String> SENSITIVE_QUERY_KEYS = Set.of(
            "token", "access_token", "password", "secret", "ak", "sk", "client_secret", "webhook_secret"
    );
    private static final Set<String> SENSITIVE_OPTION_KEYS = Set.of(
            "token", "access_token", "password", "secret", "clientsecret", "client_secret", "webhooksecret", "webhook_secret"
    );

    private final BusinessSuiteService suiteService;
    private final BusinessObjectService objectService;
    private final BusinessAppOpenService openService;

    public Page<BusinessAppVO> page(Integer pageNum, Integer pageSize, BusinessAppQueryDTO query) {
        Page<BusinessAppVO> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return baseMapper.selectAppPage(page, resolveTenantId(), normalizeQuery(query));
    }

    public List<BusinessAppVO> list(BusinessAppQueryDTO query) {
        return baseMapper.selectAppList(resolveTenantId(), normalizeQuery(query));
    }

    public BusinessAppVO detail(Long id) {
        BusinessAppVO vo = baseMapper.selectAppDetail(resolveTenantId(), id);
        if (vo == null) {
            throw new BusinessException("应用入口不存在");
        }
        return vo;
    }

    public BusinessAppOpenInfoVO openInfo(Long id) {
        return openService.openInfo(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(BusinessAppDTO dto) {
        if (dto == null) {
            throw new BusinessException("应用入口不能为空");
        }
        AiBusinessApp app = new AiBusinessApp();
        copyDtoToEntity(dto, app, true);
        save(app);
        return app.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(BusinessAppDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("应用入口ID不能为空");
        }
        AiBusinessApp app = requireEntity(dto.getId());
        copyDtoToEntity(dto, app, false);
        updateById(app);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        AiBusinessApp app = requireEntity(id);
        app.setStatus(normalizeStatus(status));
        updateById(app);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiBusinessApp app = requireEntity(id);
        removeById(app.getId());
    }

    public AiBusinessApp requireEntity(Long id) {
        if (id == null) {
            throw new BusinessException("应用入口ID不能为空");
        }
        AiBusinessApp app = baseMapper.selectEntityById(resolveTenantId(), id);
        if (app == null) {
            throw new BusinessException("应用入口不存在");
        }
        return app;
    }

    private void copyDtoToEntity(BusinessAppDTO dto, AiBusinessApp app, boolean create) {
        String appCode = StringUtils.trimToNull(dto.getAppCode());
        String appName = StringUtils.trimToNull(dto.getAppName());
        String appType = StringUtils.defaultIfBlank(dto.getAppType(), "BUSINESS").toUpperCase();
        String suiteCode = StringUtils.trimToNull(dto.getSuiteCode());
        String objectCode = StringUtils.trimToNull(dto.getObjectCode());
        String entryMode = StringUtils.defaultIfBlank(dto.getEntryMode(), "ROUTE").toUpperCase();
        if (StringUtils.isBlank(appCode) || !CODE_PATTERN.matcher(appCode).matches()) {
            throw new BusinessException("应用编码格式不正确（字母开头，仅含字母、数字和下划线，2-64字符）");
        }
        if (StringUtils.isBlank(appName)) {
            throw new BusinessException("应用名称不能为空");
        }
        if (!APP_TYPES.contains(appType)) {
            throw new BusinessException("应用类型不正确");
        }
        if (!ENTRY_MODES.contains(entryMode)) {
            throw new BusinessException("入口模式不正确");
        }
        suiteService.requireByCode(suiteCode);
        if ("BUSINESS".equals(appType) && StringUtils.isBlank(objectCode)) {
            throw new BusinessException("标准业务应用必须关联业务对象");
        }
        if (StringUtils.isNotBlank(objectCode)) {
            objectService.requireByCode(suiteCode, objectCode);
        }
        Long excludeId = create ? null : app.getId();
        if (baseMapper.countByAppCode(resolveTenantId(), appCode, excludeId) > 0) {
            throw new BusinessException("应用编码已存在: " + appCode);
        }
        validateNoSensitiveEntryConfig(dto.getEntryUrl(), dto.getOptions());
        app.setTenantId(resolveTenantId());
        app.setAppCode(appCode);
        app.setAppName(appName);
        app.setAppType(appType);
        app.setSuiteCode(suiteCode);
        app.setObjectCode(objectCode);
        app.setEntryMode(entryMode);
        app.setEntryUrl(StringUtils.trimToNull(dto.getEntryUrl()));
        app.setConfigKey(StringUtils.trimToNull(dto.getConfigKey()));
        app.setIcon(StringUtils.trimToNull(dto.getIcon()));
        app.setDescription(StringUtils.trimToNull(dto.getDescription()));
        app.setStatus(normalizeStatus(dto.getStatus()));
        app.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        app.setOptions(StringUtils.trimToNull(dto.getOptions()));
    }

    private BusinessAppQueryDTO normalizeQuery(BusinessAppQueryDTO query) {
        BusinessAppQueryDTO result = query == null ? new BusinessAppQueryDTO() : query;
        result.setKeyword(StringUtils.trimToNull(result.getKeyword()));
        result.setSuiteCode(StringUtils.trimToNull(result.getSuiteCode()));
        result.setObjectCode(StringUtils.trimToNull(result.getObjectCode()));
        result.setAppType(StringUtils.trimToNull(result.getAppType()));
        result.setEntryMode(StringUtils.trimToNull(result.getEntryMode()));
        return result;
    }

    private void validateNoSensitiveEntryConfig(String entryUrl, String options) {
        validateNoSensitiveUrl(entryUrl);
        validateNoSensitiveOptions(options);
    }

    private void validateNoSensitiveUrl(String entryUrl) {
        String url = StringUtils.trimToNull(entryUrl);
        if (StringUtils.isBlank(url) || StringUtils.startsWith(url, "/")) {
            return;
        }
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return;
        }
        if (StringUtils.isNotBlank(uri.getUserInfo())) {
            throw new BusinessException("应用入口地址不能包含用户名或密码");
        }
        String query = uri.getRawQuery();
        if (StringUtils.isBlank(query)) {
            return;
        }
        boolean containsSensitiveKey = Arrays.stream(query.split("&"))
                .map(item -> StringUtils.substringBefore(item, "="))
                .map(StringUtils::lowerCase)
                .anyMatch(SENSITIVE_QUERY_KEYS::contains);
        if (containsSensitiveKey) {
            throw new BusinessException("应用入口地址不能包含长期 Token、密码或密钥");
        }
    }

    private void validateNoSensitiveOptions(String options) {
        if (StringUtils.isBlank(options)) {
            return;
        }
        String lowerOptions = StringUtils.lowerCase(options);
        boolean containsSensitiveKey = SENSITIVE_OPTION_KEYS.stream()
                .anyMatch(key -> lowerOptions.contains("\"" + key + "\"") || lowerOptions.contains(key + "="));
        if (containsSensitiveKey) {
            throw new BusinessException("应用入口配置不能保存明文密码、Token 或 Webhook Secret");
        }
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
