package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.constant.BusinessAppMode;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 业务应用平台访问入口服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessAppService extends ServiceImpl<BusinessAppMapper, AiBusinessApp> {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{1,63}$");
    private static final Set<String> APP_TYPES = Set.of("BUSINESS", "EMBEDDED", "MOBILE", "INTEGRATION");
    private static final Set<String> ENTRY_MODES = Set.of("RUNTIME", "ROUTE", "IFRAME", "EXTERNAL", "H5", "API");
    private static final Set<String> RUNTIME_OPEN_MODES = Set.of("LIST", "CREATE_FORM", "DETAIL");
    private static final Set<String> SENSITIVE_QUERY_KEYS = Set.of(
            "token", "access_token", "password", "secret", "ak", "sk", "client_secret", "webhook_secret"
    );
    private static final Set<String> SENSITIVE_OPTION_KEYS = Set.of(
            "token", "access_token", "password", "secret", "clientsecret", "client_secret", "webhooksecret", "webhook_secret"
    );

    private final BusinessSuiteService suiteService;
    private final BusinessObjectService objectService;
    private final BusinessApplicationService applicationService;
    private final BusinessAppOpenService openService;
    private final MenuRegisterAdapter menuRegisterAdapter;

    public Page<BusinessAppVO> page(Integer pageNum, Integer pageSize, BusinessAppQueryDTO query) {
        Page<BusinessAppVO> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        Page<BusinessAppVO> result = baseMapper.selectAppPage(page, resolveTenantId(), normalizeQuery(query));
        result.getRecords().forEach(this::enrichAppVO);
        return result;
    }

    public List<BusinessAppVO> list(BusinessAppQueryDTO query) {
        List<BusinessAppVO> list = baseMapper.selectAppList(resolveTenantId(), normalizeQuery(query));
        list.forEach(this::enrichAppVO);
        return list;
    }

    public BusinessAppVO detail(Long id) {
        BusinessAppVO vo = baseMapper.selectAppDetail(resolveTenantId(), id);
        if (vo == null) {
            throw new BusinessException("访问入口不存在");
        }
        enrichAppVO(vo);
        return vo;
    }

    public BusinessAppOpenInfoVO openInfo(Long id) {
        return openService.openInfo(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(BusinessAppDTO dto) {
        if (dto == null) {
            throw new BusinessException("访问入口不能为空");
        }
        AiBusinessApp app = new AiBusinessApp();
        copyDtoToEntity(dto, app, true);
        save(app);
        syncManagementMenu(app);
        applicationService.markCompositionChanged(app.getApplicationId());
        return app.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(BusinessAppDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("访问入口ID不能为空");
        }
        AiBusinessApp app = requireEntity(dto.getId());
        Long previousApplicationId = app.getApplicationId();
        copyDtoToEntity(dto, app, false);
        updateById(app);
        syncManagementMenu(app);
        applicationService.markCompositionChanged(previousApplicationId);
        if (!java.util.Objects.equals(previousApplicationId, app.getApplicationId())) {
            applicationService.markCompositionChanged(app.getApplicationId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        AiBusinessApp app = requireEntity(id);
        app.setStatus(normalizeStatus(status));
        updateById(app);
        syncManagementMenu(app);
        applicationService.markCompositionChanged(app.getApplicationId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncRuntimeAppsForObject(String suiteCode, String objectCode, String configKey) {
        String normalizedSuiteCode = StringUtils.trimToNull(suiteCode);
        String normalizedObjectCode = StringUtils.trimToNull(objectCode);
        if (normalizedSuiteCode == null || normalizedObjectCode == null) {
            return;
        }
        List<AiBusinessApp> apps = baseMapper.selectRuntimeAppsByObject(
                resolveTenantId(), normalizedSuiteCode, normalizedObjectCode);
        for (AiBusinessApp app : apps) {
            if (StringUtils.isNotBlank(configKey) && !StringUtils.equals(configKey, app.getConfigKey())) {
                app.setConfigKey(configKey);
                updateById(app);
            }
            syncManagementMenu(app);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiBusinessApp app = requireEntity(id);
        Long applicationId = app.getApplicationId();
        deleteManagementMenu(app);
        removeById(app.getId());
        applicationService.markCompositionChanged(applicationId);
    }

    public List<AiBusinessApp> listByApplicationId(Long applicationId) {
        applicationService.requireEntity(applicationId);
        return baseMapper.selectByApplicationId(resolveTenantId(), applicationId);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Long> publishEntries(Long applicationId, List<Long> selectedEntryIds) {
        applicationService.requireEntity(applicationId);
        List<AiBusinessApp> entries = baseMapper.selectByApplicationId(resolveTenantId(), applicationId);
        Set<Long> selected = selectedEntryIds == null || selectedEntryIds.isEmpty()
                ? entries.stream().map(AiBusinessApp::getId).collect(java.util.stream.Collectors.toSet())
                : new HashSet<>(selectedEntryIds);
        List<Long> published = new ArrayList<>();
        for (AiBusinessApp entry : entries) {
            if (!selected.contains(entry.getId())) {
                continue;
            }
            if (!Integer.valueOf(1).equals(entry.getStatus())) {
                throw new BusinessException("访问入口未启用: " + entry.getAppName());
            }
            if ("RUNTIME".equalsIgnoreCase(entry.getEntryMode()) && StringUtils.isBlank(entry.getConfigKey())) {
                throw new BusinessException("运行入口缺少已发布页面配置: " + entry.getAppName());
            }
            syncManagementMenu(entry);
            published.add(entry.getId());
        }
        if (published.size() != selected.size()) {
            throw new BusinessException("发布选择中包含不属于当前应用的访问入口");
        }
        return published;
    }

    @Transactional(rollbackFor = Exception.class)
    public void restoreSnapshotEntries(Long applicationId, List<Map<String, Object>> snapshots) {
        applicationService.requireEntity(applicationId);
        Map<Long, AiBusinessApp> current = new LinkedHashMap<>();
        for (AiBusinessApp entry : baseMapper.selectByApplicationId(resolveTenantId(), applicationId)) {
            current.put(entry.getId(), entry);
        }
        for (Map<String, Object> snapshot : snapshots == null ? List.<Map<String, Object>>of() : snapshots) {
            Long id = readLong(snapshot.get("id"));
            AiBusinessApp entry = current.get(id);
            if (entry == null) {
                throw new BusinessException("历史版本依赖的访问入口已不存在: " + id);
            }
            entry.setAppName(StringUtils.trimToNull(text(snapshot.get("appName"))));
            entry.setAppType(StringUtils.trimToNull(text(snapshot.get("appType"))));
            entry.setSuiteCode(StringUtils.trimToNull(text(snapshot.get("suiteCode"))));
            entry.setObjectCode(StringUtils.trimToNull(text(snapshot.get("objectCode"))));
            entry.setEntryMode(StringUtils.trimToNull(text(snapshot.get("entryMode"))));
            entry.setEntryUrl(StringUtils.trimToNull(text(snapshot.get("entryUrl"))));
            entry.setConfigKey(StringUtils.trimToNull(text(snapshot.get("configKey"))));
            entry.setIcon(StringUtils.trimToNull(text(snapshot.get("icon"))));
            entry.setDescription(StringUtils.trimToNull(text(snapshot.get("description"))));
            entry.setStatus(readInteger(snapshot.get("status"), 1));
            entry.setSortOrder(readInteger(snapshot.get("sortOrder"), 0));
            entry.setOptions(normalizeSnapshotOptions(snapshot.get("options")));
            updateById(entry);
            syncManagementMenu(entry);
        }
    }

    private String normalizeSnapshotOptions(Object value) {
        if (value == null) {
            return null;
        }
        JSONObject options = value instanceof Map<?, ?> map
                ? new JSONObject((Map<String, Object>) map)
                : readOptions(String.valueOf(value));
        String normalized = writeOptions(options);
        validateNoSensitiveEntryConfig(null, normalized);
        return normalized;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public AiBusinessApp requireEntity(Long id) {
        if (id == null) {
            throw new BusinessException("访问入口ID不能为空");
        }
        AiBusinessApp app = baseMapper.selectEntityById(resolveTenantId(), id);
        if (app == null) {
            throw new BusinessException("访问入口不存在");
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
        String configKey = StringUtils.trimToNull(dto.getConfigKey());
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
            throw new BusinessException("标准业务应用必须关联业务单元");
        }
        if (StringUtils.isNotBlank(objectCode)) {
            objectService.requireByCode(suiteCode, objectCode);
        }
        Long excludeId = create ? null : app.getId();
        if (baseMapper.countByAppCode(resolveTenantId(), appCode, excludeId) > 0) {
            throw new BusinessException("应用编码已存在: " + appCode);
        }
        JSONObject options = readOptions(dto.getOptions());
        String runtimeOpenMode = resolveRuntimeOpenMode(firstNonNull(dto.getRuntimeOpenMode(), options.get("runtimeOpenMode")));
        if ("RUNTIME".equals(entryMode)) {
            options.put("runtimeOpenMode", runtimeOpenMode);
            String appMode = BusinessAppMode.normalize(firstNonNull(dto.getAppMode(), options.get("appMode")));
            if (BusinessAppMode.CODE_DOWNLOAD.equals(appMode)) {
                if (StringUtils.isBlank(objectCode)) {
                    throw new BusinessException("下载代码模式需要关联业务单元");
                }
                if (StringUtils.isBlank(configKey)) {
                    throw new BusinessException("下载代码模式需要选择业务页面配置");
                }
            }
            options.put("appMode", appMode);
        } else {
            options.remove("runtimeOpenMode");
            options.remove("appMode");
        }
        String normalizedOptions = writeOptions(options);
        validateNoSensitiveEntryConfig(dto.getEntryUrl(), normalizedOptions);
        Long applicationId = create || dto.getApplicationId() != null
                ? dto.getApplicationId()
                : app.getApplicationId();
        applicationService.assertEntryScope(applicationId, suiteCode, objectCode);
        app.setTenantId(resolveTenantId());
        app.setAppCode(appCode);
        app.setAppName(appName);
        app.setAppType(appType);
        app.setApplicationId(applicationId);
        app.setSuiteCode(suiteCode);
        app.setObjectCode(objectCode);
        app.setEntryMode(entryMode);
        app.setEntryUrl(StringUtils.trimToNull(dto.getEntryUrl()));
        app.setConfigKey(configKey);
        app.setIcon(StringUtils.trimToNull(dto.getIcon()));
        app.setDescription(StringUtils.trimToNull(dto.getDescription()));
        app.setStatus(normalizeStatus(dto.getStatus()));
        app.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        app.setOptions(normalizedOptions);
    }

    private void syncManagementMenu(AiBusinessApp app) {
        JSONObject options = readOptions(app.getOptions());
        JSONObject adminMenu = readAdminMenu(options);
        Long menuResourceId = readLong(firstNonNull(adminMenu.get("menuResourceId"), options.get("menuResourceId")));
        if (!Integer.valueOf(1).equals(app.getStatus())) {
            if (menuResourceId != null) {
                menuRegisterAdapter.disableMenu(menuResourceId);
            }
            return;
        }
        if (!isManagementMenuEnabled(app, options, adminMenu)) {
            detachManagementMenuIfExists(menuResourceId);
            adminMenu.remove("menuResourceId");
            adminMenu.remove("activeMenuKey");
            adminMenu.remove("actualParentId");
            adminMenu.remove("suiteMenuResourceId");
            if (adminMenu.isEmpty()) {
                options.remove("adminMenu");
            } else {
                options.put("adminMenu", adminMenu);
            }
            app.setOptions(writeOptions(options));
            updateById(app);
            return;
        }

        Long originalParentId = readLong(firstNonNull(
                adminMenu.get("originalParentId"),
                firstNonNull(adminMenu.get("parentId"), options.get("adminMenuParentId"))));
        boolean suiteAsParent = readBoolean(firstNonNull(adminMenu.get("suiteAsParent"), options.get("suiteAsMenuParent")), true);
        if (suiteAsParent) {
            originalParentId = normalizeSuiteMenuParentId(
                    originalParentId,
                    readLong(adminMenu.get("suiteMenuResourceId")),
                    readLong(adminMenu.get("actualParentId")),
                    menuResourceId);
        }
        Long parentId = originalParentId;
        Integer sort = readInteger(firstNonNull(adminMenu.get("sort"), options.get("menuSort")), app.getSortOrder());
        if (suiteAsParent) {
            parentId = suiteService.resolveOrCreateSuiteMenuDirectory(app.getSuiteCode(), parentId);
        }
        Long actualParentId = parentId;

        String path = resolveManagementMenuPath(app, options);
        String component = resolveManagementMenuComponent(app, options);
        String perms = buildAppMenuPerms(app);
        boolean enabled = Integer.valueOf(1).equals(app.getStatus());
        if (menuResourceId == null) {
            menuResourceId = menuRegisterAdapter.registerAppMenu(
                    app.getAppName(), parentId, path, component, perms, app.getIcon(), sort, enabled);
        } else {
            menuRegisterAdapter.updateAppMenu(
                    menuResourceId, app.getAppName(), parentId, path, component, perms, app.getIcon(), sort, enabled);
        }
        adminMenu.put("menuResourceId", menuResourceId == null ? null : String.valueOf(menuResourceId));
        adminMenu.put("activeMenuKey", menuResourceId == null ? null : String.valueOf(menuResourceId));
        adminMenu.put("parentId", originalParentId == null ? null : String.valueOf(originalParentId));
        adminMenu.put("originalParentId", originalParentId == null ? null : String.valueOf(originalParentId));
        if (actualParentId == null) {
            adminMenu.remove("actualParentId");
        } else {
            adminMenu.put("actualParentId", String.valueOf(actualParentId));
        }
        if (suiteAsParent && actualParentId != null) {
            adminMenu.put("suiteMenuResourceId", String.valueOf(actualParentId));
        } else {
            adminMenu.remove("suiteMenuResourceId");
        }
        adminMenu.put("suiteAsParent", suiteAsParent);
        adminMenu.put("syncEnabled", true);
        adminMenu.put("sort", sort);
        adminMenu.put("path", path);
        adminMenu.put("component", component);
        options.put("adminMenu", adminMenu);
        app.setOptions(writeOptions(options));
        updateById(app);
    }

    private void deleteManagementMenu(AiBusinessApp app) {
        JSONObject options = readOptions(app.getOptions());
        JSONObject adminMenu = readAdminMenu(options);
        Long menuResourceId = readLong(firstNonNull(adminMenu.get("menuResourceId"), options.get("menuResourceId")));
        removeManagementMenuIfExists(menuResourceId);
    }

    private void detachManagementMenuIfExists(Long menuResourceId) {
        if (menuResourceId == null) {
            return;
        }
        if (menuRegisterAdapter.hasRolePermission(menuResourceId)) {
            menuRegisterAdapter.disableMenu(menuResourceId);
            return;
        }
        menuRegisterAdapter.deleteMenu(menuResourceId);
    }

    private void removeManagementMenuIfExists(Long menuResourceId) {
        if (menuResourceId == null) {
            return;
        }
        if (menuRegisterAdapter.hasRolePermission(menuResourceId)) {
            throw new BusinessException("该访问入口关联的菜单已被角色赋权，请先在角色管理中移除授权后再操作");
        }
        menuRegisterAdapter.deleteMenu(menuResourceId);
    }

    private boolean isManagementMenuEnabled(AiBusinessApp app, JSONObject options, JSONObject adminMenu) {
        if (isCodeDownloadRuntime(app, options)) {
            return false;
        }
        String mountTarget = StringUtils.defaultIfBlank(options.getString("mountTarget"), deriveMountTarget(app));
        boolean syncEnabled = readBoolean(firstNonNull(adminMenu.get("syncEnabled"), options.get("adminMenuSyncEnabled")), false);
        return "ADMIN".equalsIgnoreCase(mountTarget) && syncEnabled;
    }

    private boolean isCodeDownloadRuntime(AiBusinessApp app, JSONObject options) {
        String entryMode = StringUtils.defaultString(app.getEntryMode()).toUpperCase();
        return "RUNTIME".equals(entryMode) && BusinessAppMode.isCodeDownload(options == null ? null : options.get("appMode"));
    }

    private String buildAppMenuPerms(AiBusinessApp app) {
        String appCode = StringUtils.defaultIfBlank(app.getAppCode(), String.valueOf(app.getId()));
        return "ai:businessApp:open:" + StringUtils.lowerCase(appCode);
    }

    private String resolveManagementMenuPath(AiBusinessApp app, JSONObject options) {
        String entryMode = StringUtils.defaultString(app.getEntryMode()).toUpperCase();
        if ("RUNTIME".equals(entryMode) && StringUtils.isNotBlank(app.getConfigKey())) {
            if (BusinessAppMode.isCodeDownload(options == null ? null : options.get("appMode"))) {
                return "/app-center?codeAppId=" + app.getId();
            }
            String runtimeOpenMode = resolveRuntimeOpenMode(options == null ? null : options.get("runtimeOpenMode"));
            StringBuilder path = new StringBuilder("/ai/crud-page/")
                    .append(app.getConfigKey())
                    .append("?appId=")
                    .append(app.getId())
                    .append("&runtimeOpenMode=")
                    .append(runtimeOpenMode);
            if ("CREATE_FORM".equals(runtimeOpenMode)) {
                path.append("&mode=create");
            } else if ("DETAIL".equals(runtimeOpenMode)) {
                path.append("&mode=detail");
            }
            return path.toString();
        }
        return "/app-center/app/" + app.getId();
    }

    private String resolveManagementMenuComponent(AiBusinessApp app, JSONObject options) {
        String entryMode = StringUtils.defaultString(app.getEntryMode()).toUpperCase();
        if ("RUNTIME".equals(entryMode) && StringUtils.isNotBlank(app.getConfigKey())) {
            if (BusinessAppMode.isCodeDownload(options == null ? null : options.get("appMode"))) {
                return "app-center/index";
            }
            return "ai/crud-page";
        }
        return "app-center/app-entry";
    }

    private String deriveMountTarget(AiBusinessApp app) {
        String appType = StringUtils.defaultString(app.getAppType()).toUpperCase();
        String entryMode = StringUtils.defaultString(app.getEntryMode()).toUpperCase();
        if ("MOBILE".equals(appType) || "H5".equals(entryMode)) {
            return "MOBILE";
        }
        if ("INTEGRATION".equals(appType) || "API".equals(entryMode)) {
            return "API";
        }
        return "ADMIN";
    }

    private JSONObject readOptions(String options) {
        if (StringUtils.isBlank(options)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(options);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private JSONObject readAdminMenu(JSONObject options) {
        JSONObject adminMenu = options.getJSONObject("adminMenu");
        return adminMenu == null ? new JSONObject() : adminMenu;
    }

    private void enrichAppVO(BusinessAppVO vo) {
        if (vo == null) {
            return;
        }
        JSONObject options = readOptions(vo.getOptions());
        JSONObject adminMenu = readAdminMenu(options);
        vo.setRuntimeOpenMode(resolveRuntimeOpenMode(options.get("runtimeOpenMode")));
        vo.setAppMode(resolveAppMode(options, vo.getEntryMode()));
        vo.setMenuResourceId(readLong(firstNonNull(adminMenu.get("menuResourceId"), options.get("menuResourceId"))));
        Object activeMenuKey = firstNonNull(adminMenu.get("activeMenuKey"), vo.getMenuResourceId());
        vo.setActiveMenuKey(activeMenuKey == null ? null : StringUtils.trimToNull(String.valueOf(activeMenuKey)));
        Long adminMenuParentId = readLong(firstNonNull(
                adminMenu.get("originalParentId"),
                firstNonNull(adminMenu.get("parentId"), options.get("adminMenuParentId"))));
        Long actualParentId = readLong(adminMenu.get("actualParentId"));
        Long suiteMenuResourceId = readLong(adminMenu.get("suiteMenuResourceId"));
        boolean suiteAsParent = readBoolean(firstNonNull(adminMenu.get("suiteAsParent"), options.get("suiteAsMenuParent")), true);
        if (suiteAsParent) {
            adminMenuParentId = normalizeSuiteMenuParentId(
                    adminMenuParentId, suiteMenuResourceId, actualParentId, vo.getMenuResourceId());
        }
        vo.setAdminMenuParentId(adminMenuParentId);
        vo.setAdminMenuActualParentId(actualParentId);
        vo.setSuiteMenuResourceId(suiteMenuResourceId);
        boolean codeDownloadRuntime = "RUNTIME".equals(StringUtils.defaultString(vo.getEntryMode()).toUpperCase())
                && BusinessAppMode.isCodeDownload(options.get("appMode"));
        vo.setAdminMenuSyncEnabled(!codeDownloadRuntime
                && readBoolean(firstNonNull(adminMenu.get("syncEnabled"), options.get("adminMenuSyncEnabled")), false));
        vo.setSuiteAsMenuParent(suiteAsParent);
        vo.setMenuSort(readInteger(firstNonNull(adminMenu.get("sort"), options.get("menuSort")), vo.getSortOrder()));
    }

    private Long normalizeSuiteMenuParentId(Long parentId, Long suiteMenuResourceId, Long actualParentId,
                                            Long menuResourceId) {
        if (parentId == null) {
            return null;
        }
        if (isSameResource(parentId, suiteMenuResourceId)
                || isSameResource(parentId, actualParentId)
                || isSameResource(parentId, menuResourceId)) {
            return null;
        }
        return parentId;
    }

    private boolean isSameResource(Long left, Long right) {
        return left != null && right != null && left.equals(right);
    }

    private String writeOptions(JSONObject options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        return options.toJSONString();
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private Long readLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer readInteger(Object value, Integer fallback) {
        if (value == null) {
            return fallback == null ? 0 : fallback;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return fallback == null ? 0 : fallback;
        }
    }

    private boolean readBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = StringUtils.lowerCase(String.valueOf(value));
        return "true".equals(text) || "1".equals(text);
    }

    private String resolveRuntimeOpenMode(Object value) {
        String mode = StringUtils.defaultIfBlank(value == null ? null : String.valueOf(value), "LIST").toUpperCase();
        return RUNTIME_OPEN_MODES.contains(mode) ? mode : "LIST";
    }

    private String resolveAppMode(JSONObject options, String entryMode) {
        if (!"RUNTIME".equals(StringUtils.defaultString(entryMode).toUpperCase())) {
            return null;
        }
        return BusinessAppMode.normalize(options == null ? null : options.get("appMode"));
    }

    private BusinessAppQueryDTO normalizeQuery(BusinessAppQueryDTO query) {
        BusinessAppQueryDTO result = query == null ? new BusinessAppQueryDTO() : query;
        result.setKeyword(StringUtils.trimToNull(result.getKeyword()));
        result.setSuiteCode(StringUtils.trimToNull(result.getSuiteCode()));
        result.setSuiteCodes(normalizeSuiteCodes(result.getSuiteCodes()));
        result.setObjectCode(StringUtils.trimToNull(result.getObjectCode()));
        result.setAppType(StringUtils.trimToNull(result.getAppType()));
        result.setEntryMode(StringUtils.trimToNull(result.getEntryMode()));
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
            throw new BusinessException("访问入口地址不能包含用户名或密码");
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
            throw new BusinessException("访问入口地址不能包含长期 Token、密码或密钥");
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
            throw new BusinessException("访问入口配置不能保存明文密码、Token 或 Webhook Secret");
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
