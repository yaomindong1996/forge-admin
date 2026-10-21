package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.constant.BusinessAppMode;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessSuite;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessSuiteDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessSuiteQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessSuiteMapper;
import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessSuiteSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessSuiteVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/**
 * 业务应用平台业务套件服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessSuiteService extends ServiceImpl<BusinessSuiteMapper, AiBusinessSuite> {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{1,63}$");

    private final MenuRegisterAdapter menuRegisterAdapter;
    private final BusinessAppMapper businessAppMapper;
    private final BusinessApplicationMapper businessApplicationMapper;

    public Page<BusinessSuiteVO> page(Integer pageNum, Integer pageSize, BusinessSuiteQueryDTO query) {
        Page<BusinessSuiteVO> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return baseMapper.selectSuitePage(page, resolveTenantId(), normalizeQuery(query));
    }

    public List<BusinessSuiteVO> list(BusinessSuiteQueryDTO query) {
        return baseMapper.selectSuiteList(resolveTenantId(), normalizeQuery(query));
    }

    public BusinessSuiteVO detail(Long id) {
        BusinessSuiteVO vo = baseMapper.selectSuiteDetail(resolveTenantId(), id);
        if (vo == null) {
            throw new BusinessException("业务套件不存在");
        }
        return vo;
    }

    public List<BusinessSuiteSummaryVO> summary() {
        return baseMapper.selectSuiteSummary(resolveTenantId());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(BusinessSuiteDTO dto) {
        if (dto == null) {
            throw new BusinessException("业务套件不能为空");
        }
        AiBusinessSuite suite = new AiBusinessSuite();
        copyDtoToEntity(dto, suite, true);
        save(suite);
        syncMenuStatusBySuiteStatus(suite);
        return suite.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(BusinessSuiteDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("业务套件ID不能为空");
        }
        AiBusinessSuite suite = requireEntity(dto.getId());
        copyDtoToEntity(dto, suite, false);
        updateById(suite);
        syncMenuStatusBySuiteStatus(suite);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        AiBusinessSuite suite = requireEntity(id);
        suite.setStatus(normalizeStatus(status));
        updateById(suite);
        syncMenuStatusBySuiteStatus(suite);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, boolean cleanupOrphanResources) {
        AiBusinessSuite suite = requireEntity(id);
        Long tenantId = resolveTenantId();
        if (baseMapper.countChildrenBySuite(tenantId, suite.getId()) > 0) {
            throw new BusinessException("该业务域已存在子业务域，不能删除");
        }
        if (businessApplicationMapper.countBySuiteCode(tenantId, suite.getSuiteCode()) > 0) {
            throw new BusinessException("该业务域下仍有业务应用，请先删除或迁移应用");
        }
        Long entryCount = baseMapper.countAppsBySuite(tenantId, suite.getSuiteCode());
        Long objectCount = baseMapper.countObjectsBySuite(tenantId, suite.getSuiteCode());
        if ((entryCount > 0 || objectCount > 0) && !cleanupOrphanResources) {
            List<String> orphanResources = new ArrayList<>();
            if (entryCount > 0) {
                orphanResources.add(entryCount + " 个访问入口");
            }
            if (objectCount > 0) {
                orphanResources.add(objectCount + " 个业务对象");
            }
            throw new BusinessException("该业务域还保留 " + String.join("、", orphanResources)
                    + "，请确认同时清理后再删除");
        }
        if (entryCount > 0) {
            if (baseMapper.countActiveApplicationEntryReferencesBySuite(tenantId, suite.getSuiteCode()) > 0) {
                throw new BusinessException("该业务域下的访问入口仍被业务应用使用，不能删除");
            }
        }
        if (objectCount > 0) {
            if (baseMapper.countActiveApplicationObjectReferencesBySuite(tenantId, suite.getSuiteCode()) > 0) {
                throw new BusinessException("该业务域下的业务对象仍被业务应用使用，不能删除");
            }
        }
        if (entryCount > 0) {
            disableAppMenus(List.of(suite.getSuiteCode()));
            baseMapper.logicDeleteEntriesBySuite(tenantId, suite.getSuiteCode());
        }
        if (objectCount > 0) {
            baseMapper.deleteObjectRelationsBySuite(tenantId, suite.getSuiteCode());
            baseMapper.logicDeleteObjectsBySuite(tenantId, suite.getSuiteCode());
        }
        removeById(suite.getId());
    }

    public AiBusinessSuite requireByCode(String suiteCode) {
        String code = StringUtils.trimToNull(suiteCode);
        if (StringUtils.isBlank(code)) {
            throw new BusinessException("业务套件编码不能为空");
        }
        AiBusinessSuite suite = baseMapper.selectBySuiteCode(resolveTenantId(), code);
        if (suite == null) {
            throw new BusinessException("业务套件不存在: " + code);
        }
        return suite;
    }

    /**
     * 调试包 / 应用宝导入：套件编码已在包内声明时，本地没有则自动创建，避免导入因缺套件直接失败。
     */
    @Transactional(rollbackFor = Exception.class)
    public AiBusinessSuite ensureByCode(String suiteCode, String suiteName) {
        String code = StringUtils.trimToNull(suiteCode);
        if (StringUtils.isBlank(code)) {
            throw new BusinessException("业务套件编码不能为空");
        }
        AiBusinessSuite existing = baseMapper.selectBySuiteCode(resolveTenantId(), code);
        if (existing != null) {
            return existing;
        }
        BusinessSuiteDTO dto = new BusinessSuiteDTO();
        dto.setSuiteCode(code);
        dto.setSuiteName(StringUtils.defaultIfBlank(StringUtils.trimToNull(suiteName), code));
        dto.setDescription("应用调试包导入时自动创建");
        dto.setStatus(EnableStatus.ENABLED.getCode());
        dto.setSortOrder(99);
        dto.setOptions("{\"source\":\"debug_bundle_import\"}");
        create(dto);
        return requireByCode(code);
    }

    public List<String> listSelfAndDescendantCodes(String suiteCode) {
        String code = StringUtils.trimToNull(suiteCode);
        if (code == null) {
            return List.of();
        }
        List<String> codes = baseMapper.selectSelfAndDescendantCodes(resolveTenantId(), code);
        if (codes == null || codes.isEmpty()) {
            throw new BusinessException("业务套件不存在: " + code);
        }
        return codes;
    }

    public AiBusinessSuite requireEntity(Long id) {
        if (id == null) {
            throw new BusinessException("业务套件ID不能为空");
        }
        AiBusinessSuite suite = getById(id);
        if (suite == null) {
            throw new BusinessException("业务套件不存在");
        }
        return suite;
    }

    private void copyDtoToEntity(BusinessSuiteDTO dto, AiBusinessSuite suite, boolean create) {
        String suiteCode = StringUtils.trimToNull(dto.getSuiteCode());
        String suiteName = StringUtils.trimToNull(dto.getSuiteName());
        if (StringUtils.isBlank(suiteCode) || !CODE_PATTERN.matcher(suiteCode).matches()) {
            throw new BusinessException("套件编码格式不正确（字母开头，仅含字母、数字和下划线，2-64字符）");
        }
        if (StringUtils.isBlank(suiteName)) {
            throw new BusinessException("套件名称不能为空");
        }
        Long excludeId = create ? null : suite.getId();
        if (baseMapper.countBySuiteCode(resolveTenantId(), suiteCode, excludeId) > 0) {
            throw new BusinessException("套件编码已存在: " + suiteCode);
        }
        Long parentId = normalizeParentId(dto.getParentId(), excludeId);
        suite.setTenantId(resolveTenantId());
        suite.setParentId(parentId);
        suite.setSuiteCode(suiteCode);
        suite.setSuiteName(suiteName);
        suite.setIcon(StringUtils.trimToNull(dto.getIcon()));
        suite.setDescription(StringUtils.trimToNull(dto.getDescription()));
        suite.setStatus(normalizeStatus(dto.getStatus()));
        suite.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        suite.setOptions(StringUtils.trimToNull(dto.getOptions()));
    }

    private void syncMenuDirectory(AiBusinessSuite suite) {
        JSONObject options = readOptions(suite.getOptions());
        JSONObject adminMenu = readAdminMenu(options);
        boolean syncEnabled = readBoolean(adminMenu.get("syncEnabled"), false);
        if (!syncEnabled) {
            return;
        }

        Long parentId = readLong(adminMenu.get("parentId"));
        Integer sort = readInteger(adminMenu.get("sort"), suite.getSortOrder());
        adminMenu.put("syncEnabled", true);
        adminMenu.put("parentId", parentId == null ? null : String.valueOf(parentId));
        adminMenu.put("sort", sort);
        options.put("adminMenu", adminMenu);
        suite.setOptions(writeOptions(options));
        resolveOrCreateSuiteMenuDirectory(suite, parentId, new HashSet<>());
    }

    private void syncMenuStatusBySuiteStatus(AiBusinessSuite suite) {
        if (EnableStatus.ENABLED.matches(suite.getStatus())) {
            syncMenuDirectory(suite);
            restoreEnabledAppMenus(resolveSuiteTreeCodes(suite));
            return;
        }
        List<String> suiteCodes = resolveSuiteTreeCodes(suite);
        disableSuiteMenus(suiteCodes);
        disableAppMenus(suiteCodes);
    }

    public Long resolveOrCreateSuiteMenuDirectory(String suiteCode, Long rootParentId) {
        AiBusinessSuite suite = requireByCode(suiteCode);
        return resolveOrCreateSuiteMenuDirectory(suite, rootParentId, new HashSet<>());
    }

    private Long resolveOrCreateSuiteMenuDirectory(AiBusinessSuite suite, Long rootParentId, Set<Long> resolvingIds) {
        if (suite == null) {
            return rootParentId;
        }
        if (suite.getId() != null && !resolvingIds.add(suite.getId())) {
            throw new BusinessException("业务域父级存在循环配置");
        }
        Long parentMenuId = rootParentId;
        if (suite.getParentId() != null) {
            AiBusinessSuite parent = requireEntity(suite.getParentId());
            Long parentRootMenuId = rootParentId != null ? rootParentId : readConfiguredMenuParentId(parent);
            parentMenuId = resolveOrCreateSuiteMenuDirectory(parent, parentRootMenuId, resolvingIds);
        } else if (parentMenuId == null) {
            parentMenuId = readConfiguredMenuParentId(suite);
        }
        Integer sort = readConfiguredMenuSort(suite);
        Long menuResourceId = menuRegisterAdapter.resolveOrCreateBusinessSuiteParentId(
                parentMenuId, suite.getSuiteCode(), suite.getSuiteName(), suite.getIcon(), sort);
        rememberSuiteMenuResource(suite, readConfiguredMenuParentId(suite), parentMenuId, menuResourceId, sort);
        if (suite.getId() != null) {
            resolvingIds.remove(suite.getId());
        }
        return menuResourceId;
    }

    private List<String> resolveSuiteTreeCodes(AiBusinessSuite root) {
        if (root == null || StringUtils.isBlank(root.getSuiteCode())) {
            return List.of();
        }
        List<AiBusinessSuite> suites = baseMapper.selectSuiteList(resolveTenantId(), new BusinessSuiteQueryDTO())
                .stream()
                .map(this::toEntity)
                .toList();
        Set<Long> descendantIds = new HashSet<>();
        if (root.getId() != null) {
            collectSuiteDescendantIds(root.getId(), suites, descendantIds);
        }
        return suites.stream()
                .filter(item -> item != null && StringUtils.isNotBlank(item.getSuiteCode()))
                .filter(item -> StringUtils.equals(item.getSuiteCode(), root.getSuiteCode())
                        || (item.getId() != null && descendantIds.contains(item.getId())))
                .map(AiBusinessSuite::getSuiteCode)
                .distinct()
                .toList();
    }

    private void collectSuiteDescendantIds(Long parentId, List<AiBusinessSuite> suites, Set<Long> result) {
        if (parentId == null || suites == null || suites.isEmpty()) {
            return;
        }
        for (AiBusinessSuite suite : suites) {
            if (suite == null || suite.getId() == null || !Objects.equals(parentId, suite.getParentId())
                    || !result.add(suite.getId())) {
                continue;
            }
            collectSuiteDescendantIds(suite.getId(), suites, result);
        }
    }

    private void disableSuiteMenus(List<String> suiteCodes) {
        if (suiteCodes == null || suiteCodes.isEmpty()) {
            return;
        }
        List<AiBusinessSuite> suites = baseMapper.selectSuiteList(resolveTenantId(), new BusinessSuiteQueryDTO())
                .stream()
                .map(this::toEntity)
                .filter(item -> suiteCodes.contains(item.getSuiteCode()))
                .toList();
        for (AiBusinessSuite suite : suites) {
            Long menuResourceId = readConfiguredMenuResourceId(suite);
            if (menuResourceId != null) {
                menuRegisterAdapter.disableMenu(menuResourceId);
            }
        }
    }

    private void disableAppMenus(List<String> suiteCodes) {
        for (AiBusinessApp app : selectAppsBySuiteCodes(suiteCodes)) {
            Long menuResourceId = readAppMenuResourceId(app);
            if (menuResourceId != null) {
                menuRegisterAdapter.disableMenu(menuResourceId);
            }
        }
    }

    private void restoreEnabledAppMenus(List<String> suiteCodes) {
        for (AiBusinessApp app : selectAppsBySuiteCodes(suiteCodes)) {
            if (!EnableStatus.ENABLED.matches(app.getStatus())) {
                continue;
            }
            JSONObject options = readOptions(app.getOptions());
            JSONObject adminMenu = readAdminMenu(options);
            Long menuResourceId = readLong(firstNonNull(adminMenu.get("menuResourceId"), options.get("menuResourceId")));
            if (menuResourceId == null || !isAppManagementMenuEnabled(app, options, adminMenu)) {
                continue;
            }
            Long parentId = resolveAppMenuParentId(app, options, adminMenu, menuResourceId);
            Integer sort = readInteger(firstNonNull(adminMenu.get("sort"), options.get("menuSort")), app.getSortOrder());
            String mountTarget = StringUtils.defaultIfBlank(options.getString("mountTarget"), deriveMountTarget(app));
            boolean mobileMount = "MOBILE".equalsIgnoreCase(mountTarget);
            String path = mobileMount
                    ? resolveAppMenuPath(app, options)
                    : StringUtils.defaultIfBlank(adminMenu.getString("path"), resolveAppMenuPath(app, options));
            String component = mobileMount
                    ? resolveAppMenuComponent(app, options)
                    : StringUtils.defaultIfBlank(adminMenu.getString("component"), resolveAppMenuComponent(app, options));
            menuRegisterAdapter.updateAppMenu(
                    menuResourceId,
                    app.getAppName(),
                    parentId,
                    path,
                    component,
                    buildAppMenuPerms(app),
                    app.getIcon(),
                    sort,
                    true,
                    mobileMount ? "h5" : "pc"
            );
        }
    }

    private List<AiBusinessApp> selectAppsBySuiteCodes(List<String> suiteCodes) {
        if (suiteCodes == null || suiteCodes.isEmpty()) {
            return List.of();
        }
        return businessAppMapper.selectAppsBySuiteCodes(resolveTenantId(), suiteCodes);
    }

    private Long resolveAppMenuParentId(AiBusinessApp app, JSONObject options, JSONObject adminMenu, Long menuResourceId) {
        if (isMobileMount(app, options)) {
            return 0L;
        }
        Long originalParentId = readLong(firstNonNull(
                adminMenu.get("originalParentId"),
                firstNonNull(adminMenu.get("parentId"), options.get("adminMenuParentId"))));
        boolean suiteAsParent = readBoolean(firstNonNull(adminMenu.get("suiteAsParent"), options.get("suiteAsMenuParent")), true);
        if (!suiteAsParent) {
            return normalizeSuiteMenuParentId(
                    originalParentId,
                    readLong(adminMenu.get("suiteMenuResourceId")),
                    readLong(adminMenu.get("actualParentId")),
                    menuResourceId);
        }
        Long normalizedParentId = normalizeSuiteMenuParentId(
                originalParentId,
                readLong(adminMenu.get("suiteMenuResourceId")),
                readLong(adminMenu.get("actualParentId")),
                menuResourceId);
        return resolveOrCreateSuiteMenuDirectory(app.getSuiteCode(), normalizedParentId);
    }

    private boolean isAppManagementMenuEnabled(AiBusinessApp app, JSONObject options, JSONObject adminMenu) {
        if (isCodeDownloadRuntime(app, options)) {
            return false;
        }
        String mountTarget = StringUtils.defaultIfBlank(options.getString("mountTarget"), deriveMountTarget(app));
        boolean syncEnabled = readBoolean(firstNonNull(adminMenu.get("syncEnabled"), options.get("adminMenuSyncEnabled")), false);
        return ("ADMIN".equalsIgnoreCase(mountTarget) || "MOBILE".equalsIgnoreCase(mountTarget))
                && syncEnabled;
    }

    private String resolveAppMenuPath(AiBusinessApp app, JSONObject options) {
        if (isMobileMount(app, options)) {
            return resolveMobileMenuPath(app, options);
        }
        String entryMode = StringUtils.defaultString(app.getEntryMode()).toUpperCase();
        if ("RUNTIME".equals(entryMode) && StringUtils.isNotBlank(app.getConfigKey())) {
            String runtimeOpenMode = resolveRuntimeOpenMode(options == null ? null : options.get("runtimeOpenMode"));
            StringBuilder path = new StringBuilder("/ai/crud-page/")
                    .append(app.getConfigKey())
                    .append("?appId=")
                    .append(app.getId())
                    .append("&runtimeOpenMode=")
                    .append(runtimeOpenMode);
            // 入口配置的目标页面/表单必须体现在菜单路径上，否则设计态选择不会生效。
            String targetPageKey = options == null ? null : StringUtils.trimToNull(options.getString("targetPageKey"));
            if (targetPageKey == null) {
                targetPageKey = "DETAIL".equals(runtimeOpenMode) ? "detail" : "list";
            }
            path.append("&pageKey=").append(targetPageKey);
            String targetFormKey = options == null ? null : StringUtils.trimToNull(options.getString("targetFormKey"));
            if (targetFormKey != null) {
                path.append("&formKey=").append(targetFormKey);
            }
            if ("CREATE_FORM".equals(runtimeOpenMode)) {
                path.append("&mode=create");
            } else if ("DETAIL".equals(runtimeOpenMode)) {
                path.append("&mode=detail");
            }
            return path.toString();
        }
        return "/app-center/app/" + app.getId();
    }

    private String resolveAppMenuComponent(AiBusinessApp app, JSONObject options) {
        if (isMobileMount(app, options)) {
            return resolveMobileMenuPath(app, options);
        }
        String entryMode = StringUtils.defaultString(app.getEntryMode()).toUpperCase();
        if ("RUNTIME".equals(entryMode) && StringUtils.isNotBlank(app.getConfigKey())) {
            return "ai/crud-page";
        }
        return "app-center/app-entry";
    }

    private String resolveMobileMenuPath(AiBusinessApp app, JSONObject options) {
        String entryMode = StringUtils.defaultString(app.getEntryMode()).toUpperCase();
        if ("RUNTIME".equals(entryMode) && StringUtils.isNotBlank(app.getConfigKey())) {
            String runtimeOpenMode = resolveRuntimeOpenMode(options == null ? null : options.get("runtimeOpenMode"));
            StringBuilder path = new StringBuilder("/pages/lowcode-runtime?configKey=")
                    .append(app.getConfigKey())
                    .append("&appId=")
                    .append(app.getId());
            if ("CREATE_FORM".equals(runtimeOpenMode)) {
                path.append("&mode=create");
            } else if ("DETAIL".equals(runtimeOpenMode)) {
                path.append("&mode=detail");
            }
            return path.toString();
        }
        String entryUrl = StringUtils.trimToNull(app.getEntryUrl());
        return entryUrl == null ? "/pages/app-entry" : entryUrl;
    }

    private boolean isMobileMount(AiBusinessApp app, JSONObject options) {
        String mountTarget = StringUtils.defaultIfBlank(
                options == null ? null : options.getString("mountTarget"), deriveMountTarget(app));
        return "MOBILE".equalsIgnoreCase(mountTarget);
    }

    private boolean isCodeDownloadRuntime(AiBusinessApp app, JSONObject options) {
        String entryMode = StringUtils.defaultString(app.getEntryMode()).toUpperCase();
        return "RUNTIME".equals(entryMode) && BusinessAppMode.isCodeDownload(options == null ? null : options.get("appMode"));
    }

    private String buildAppMenuPerms(AiBusinessApp app) {
        String appCode = StringUtils.defaultIfBlank(app.getAppCode(), String.valueOf(app.getId()));
        return "ai:businessApp:open:" + StringUtils.lowerCase(appCode);
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

    private String resolveRuntimeOpenMode(Object value) {
        String mode = StringUtils.defaultIfBlank(value == null ? null : String.valueOf(value), "LIST").toUpperCase();
        return Set.of("LIST", "CREATE_FORM", "DETAIL").contains(mode) ? mode : "LIST";
    }

    private Long readConfiguredMenuResourceId(AiBusinessSuite suite) {
        if (suite == null) {
            return null;
        }
        JSONObject options = readOptions(suite.getOptions());
        return readLong(readAdminMenu(options).get("menuResourceId"));
    }

    private Long readAppMenuResourceId(AiBusinessApp app) {
        if (app == null) {
            return null;
        }
        JSONObject options = readOptions(app.getOptions());
        JSONObject adminMenu = readAdminMenu(options);
        return readLong(firstNonNull(adminMenu.get("menuResourceId"), options.get("menuResourceId")));
    }

    private Long normalizeSuiteMenuParentId(Long parentId, Long suiteMenuResourceId, Long actualParentId,
                                            Long menuResourceId) {
        if (parentId == null) {
            return null;
        }
        // 仅当菜单父级指向自身（真正自父级）时才判定为配置错误返回 null；
        // parentId == actualParentId（父级即实际挂载目录）属正常层级结构，必须保留，
        // 否则会误伤"子入口挂在应用链接入口下"的场景，导致菜单被提升到顶级、不可点击。
        if (Objects.equals(parentId, menuResourceId)) {
            return null;
        }
        return parentId;
    }

    private Long normalizeParentId(Long parentId, Long currentId) {
        if (parentId == null) {
            return null;
        }
        Long tenantId = resolveTenantId();
        AiBusinessSuite parent = requireEntity(parentId);
        if (!Objects.equals(parent.getTenantId(), tenantId)) {
            throw new BusinessException("上级业务域不存在");
        }
        if (currentId == null) {
            return parentId;
        }
        if (Objects.equals(parentId, currentId)) {
            throw new BusinessException("上级业务域不能选择自己");
        }
        Map<Long, AiBusinessSuite> suiteMap = baseMapper.selectSuiteList(tenantId, new BusinessSuiteQueryDTO())
                .stream()
                .filter(item -> item.getId() != null)
                .map(this::toEntity)
                .collect(Collectors.toMap(AiBusinessSuite::getId, Function.identity(), (left, right) -> left));
        Long cursor = parentId;
        Set<Long> visited = new HashSet<>();
        while (cursor != null && visited.add(cursor)) {
            if (Objects.equals(cursor, currentId)) {
                throw new BusinessException("上级业务域不能选择自己的下级");
            }
            AiBusinessSuite cursorSuite = suiteMap.get(cursor);
            cursor = cursorSuite == null ? null : cursorSuite.getParentId();
        }
        return parentId;
    }

    private AiBusinessSuite toEntity(BusinessSuiteVO vo) {
        AiBusinessSuite suite = new AiBusinessSuite();
        suite.setId(vo.getId());
        suite.setTenantId(resolveTenantId());
        suite.setParentId(vo.getParentId());
        suite.setSuiteCode(vo.getSuiteCode());
        suite.setSuiteName(vo.getSuiteName());
        suite.setIcon(vo.getIcon());
        suite.setDescription(vo.getDescription());
        suite.setStatus(vo.getStatus());
        suite.setSortOrder(vo.getSortOrder());
        suite.setOptions(vo.getOptions());
        return suite;
    }

    private Long readConfiguredMenuParentId(AiBusinessSuite suite) {
        if (suite == null) {
            return null;
        }
        JSONObject options = readOptions(suite.getOptions());
        JSONObject adminMenu = readAdminMenu(options);
        return readLong(adminMenu.get("parentId"));
    }

    private Integer readConfiguredMenuSort(AiBusinessSuite suite) {
        if (suite == null) {
            return 0;
        }
        JSONObject options = readOptions(suite.getOptions());
        JSONObject adminMenu = readAdminMenu(options);
        return readInteger(adminMenu.get("sort"), suite.getSortOrder());
    }

    private void rememberSuiteMenuResource(AiBusinessSuite suite, Long originalParentId, Long actualParentId,
                                           Long menuResourceId, Integer sort) {
        if (suite == null || suite.getId() == null || menuResourceId == null) {
            return;
        }
        JSONObject options = readOptions(suite.getOptions());
        JSONObject adminMenu = readAdminMenu(options);
        adminMenu.put("parentId", originalParentId == null ? null : String.valueOf(originalParentId));
        if (actualParentId == null) {
            adminMenu.remove("actualParentId");
        } else {
            adminMenu.put("actualParentId", String.valueOf(actualParentId));
        }
        adminMenu.put("sort", sort == null ? 0 : sort);
        adminMenu.put("menuResourceId", String.valueOf(menuResourceId));
        if (!adminMenu.containsKey("syncEnabled")) {
            adminMenu.put("syncEnabled", true);
        }
        options.put("adminMenu", adminMenu);
        String nextOptions = writeOptions(options);
        if (!StringUtils.equals(nextOptions, suite.getOptions())) {
            AiBusinessSuite update = new AiBusinessSuite();
            update.setId(suite.getId());
            update.setOptions(nextOptions);
            updateById(update);
            suite.setOptions(nextOptions);
        }
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

    private BusinessSuiteQueryDTO normalizeQuery(BusinessSuiteQueryDTO query) {
        BusinessSuiteQueryDTO result = query == null ? new BusinessSuiteQueryDTO() : query;
        result.setKeyword(StringUtils.trimToNull(result.getKeyword()));
        result.setSuiteCode(StringUtils.trimToNull(result.getSuiteCode()));
        return result;
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
