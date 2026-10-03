package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationDesignStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationAiAssistantConfigDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDistributionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPortalConfigDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationQueryDTO;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationCreateVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/**
 * 业务应用聚合服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessApplicationService extends ServiceImpl<BusinessApplicationMapper, AiBusinessApplication> {

    private static final int APPLICATION_CODE_MAX_LENGTH = 64;
    private static final int PORTAL_SLUG_MAX_LENGTH = 50;
    private static final int GENERATED_CODE_ATTEMPTS = 1000;
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{1,63}$");
    private static final Set<String> SENSITIVE_OPTION_KEYS = Set.of(
            "token", "access_token", "password", "secret", "clientsecret", "client_secret",
            "webhooksecret", "webhook_secret", "apikey", "api_key", "ak", "sk"
    );
    private static final Set<String> RESERVED_PORTAL_SLUGS = Set.of(
            "admin", "api", "app-center", "system", "login", "logout", "auth", "file",
            "dict", "ai", "report", "flow", "h5", "mobile", "integration", "preview",
            "runtime", "static", "assets", "favicon.ico"
    );
    private static final Pattern PORTAL_SLUG_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{2,50}$");

    private final BusinessSuiteService suiteService;
    private final BusinessApplicationObjectMapper applicationObjectMapper;
    private final BusinessAppMapper businessAppMapper;
    private final BusinessNamingService namingService;
    private final MenuRegisterAdapter menuRegisterAdapter;

    public Page<BusinessApplicationVO> page(Integer pageNum, Integer pageSize, BusinessApplicationQueryDTO query) {
        Page<BusinessApplicationVO> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return baseMapper.selectApplicationPage(page, resolveTenantId(), normalizeQuery(query));
    }

    public List<BusinessApplicationVO> list(BusinessApplicationQueryDTO query) {
        return baseMapper.selectApplicationList(resolveTenantId(), normalizeQuery(query));
    }

    /**
     * 查询已发布工作台候选。投放开关必须按发布快照判定，这里只返回发布过的应用。
     */
    public List<BusinessApplicationVO> workbenchDistributionCandidates() {
        List<BusinessApplicationVO> candidates = baseMapper.selectPublishedWorkbenchApplications(resolveTenantId());
        return candidates == null ? List.of() : List.copyOf(candidates);
    }

    public BusinessApplicationVO detail(Long id) {
        BusinessApplicationVO application = baseMapper.selectApplicationDetail(resolveTenantId(), id);
        if (application == null) {
            throw new BusinessException("业务应用不存在");
        }
        return application;
    }

    /**
     * 仅加载 inAppBuilder，供审批 task-form-context 按 formKey 解析页面表单。
     * 不走 detail 的统计 JOIN，减少 DB 与网络开销。
     */
    public JSONObject loadInAppBuilder(Long applicationId) {
        if (applicationId == null) {
            return new JSONObject();
        }
        String raw = baseMapper.selectApplicationInAppBuilderJson(resolveTenantId(), applicationId);
        if (StringUtils.isBlank(raw) || "null".equalsIgnoreCase(raw.trim())) {
            return new JSONObject();
        }
        try {
            Object parsed = JSON.parse(raw);
            // MySQL JSON 抽取有时仍是 JSON 字符串字面量，再解一层
            if (parsed instanceof String text && StringUtils.isNotBlank(text)) {
                parsed = JSON.parse(text);
            }
            if (parsed instanceof JSONObject object) {
                return object;
            }
            if (parsed instanceof Map<?, ?> map) {
                JSONObject object = new JSONObject();
                map.forEach((key, value) -> {
                    if (key != null) {
                        object.put(String.valueOf(key), value);
                    }
                });
                return object;
            }
            return JSON.parseObject(String.valueOf(parsed));
        } catch (Exception error) {
            log.debug("解析应用 inAppBuilder 失败: applicationId={}, error={}", applicationId, error.getMessage());
            return new JSONObject();
        }
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

    public BusinessApplicationVO detailBySlug(String portalSlug) {
        return detailByPublishedSlug(portalSlug);
    }

    public BusinessApplicationVO detailByCodeOrSlug(String identifier) {
        String value = StringUtils.trimToNull(identifier);
        if (value == null) {
            throw new BusinessException("应用编码或门户地址不能为空");
        }
        BusinessApplicationVO application = baseMapper.selectApplicationDetailByCodeOrSlug(resolveTenantId(), value);
        if (application == null) {
            throw new BusinessException("应用门户不存在");
        }
        return application;
    }

    /** 正式门户按应用编码或当前已发布快照中的 slug 解析。 */
    public BusinessApplicationVO detailByPublishedCodeOrSlug(String identifier) {
        String value = StringUtils.trimToNull(identifier);
        if (value == null) {
            throw new BusinessException("应用编码或门户地址不能为空");
        }
        BusinessApplicationVO application = baseMapper.selectApplicationDetailByCode(
                resolveTenantId(), value);
        if (application != null) {
            return application;
        }
        return detailByPublishedSlug(value);
    }

    public BusinessApplicationVO detailByPublishedSlug(String portalSlug) {
        String slug = StringUtils.trimToNull(portalSlug);
        validatePortalSlug(slug);
        BusinessApplicationVO application = baseMapper.selectApplicationDetailByPublishedSlug(
                resolveTenantId(), slug);
        if (application == null) {
            throw new BusinessException("应用门户不存在");
        }
        return application;
    }

    /** 当前用户是否满足已发布门户配置中的应用级可见范围。 */
    public boolean canCurrentUserAccessPortal(String portalConfig) {
        LoginUser loginUser = currentLoginUser();
        if (loginUser != null && loginUser.isAdmin()) {
            return true;
        }
        Long userId = loginUser == null ? null : loginUser.getUserId();
        if (userId == null) {
            return false;
        }
        JSONObject permission = parseJsonObject(portalConfig).getJSONObject("permission");
        if (permission == null) {
            return true;
        }
        if (containsIdentifier(permission.getJSONArray("administrators"), userId)) {
            return true;
        }
        String visibility = StringUtils.defaultIfBlank(permission.getString("visibility"), "all")
                .toLowerCase(Locale.ROOT);
        return switch (visibility) {
            case "all" -> true;
            case "users" -> containsIdentifier(permission.getJSONArray("userIds"), userId);
            case "roles" -> intersects(permission.getJSONArray("roleIds"), loginUser.getRoleIds());
            case "departments" -> intersects(permission.getJSONArray("departmentIds"), loginUser.getOrgIds());
            default -> false;
        };
    }

    /** 当前用户是否为应用管理员；应用管理员绕过页面级角色权限。 */
    public boolean currentUserIsApplicationAdministrator(String portalConfig) {
        LoginUser loginUser = currentLoginUser();
        if (loginUser == null) {
            return false;
        }
        if (loginUser.isAdmin()) {
            return true;
        }
        JSONObject permission = parseJsonObject(portalConfig).getJSONObject("permission");
        return permission != null
                && containsIdentifier(permission.getJSONArray("administrators"), loginUser.getUserId());
    }

    public boolean slugAvailable(String portalSlug, Long excludeId) {
        String slug = StringUtils.trimToNull(portalSlug);
        validatePortalSlug(slug);
        Long tenantId = resolveTenantId();
        return baseMapper.countByPortalSlug(tenantId, slug, excludeId) == 0
                && baseMapper.countByApplicationCode(tenantId, slug, excludeId) == 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public void savePortalConfig(Long id, BusinessApplicationPortalConfigDTO dto) {
        AiBusinessApplication application = requireEntity(id);
        if (dto == null) {
            throw new BusinessException("门户配置不能为空");
        }
        String slug = StringUtils.defaultIfBlank(dto.getPortalSlug(), application.getPortalSlug());
        if (StringUtils.isBlank(slug)) {
            slug = defaultPortalSlug(application.getApplicationCode());
        }
        validatePortalSlug(slug);
        assertPortalSlugAvailable(slug, id);
        application.setPortalSlug(slug);
        application.setPortalConfig(normalizeJsonObject(dto.getPortalConfig(), "门户配置", true));
        assertPortalPermissionIdentifiers(application.getPortalConfig());
        updateById(application);
        baseMapper.markChanged(resolveTenantId(), id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveAiAssistantConfig(Long id, BusinessApplicationAiAssistantConfigDTO dto) {
        AiBusinessApplication application = requireEntity(id);
        if (dto == null) {
            throw new BusinessException("AI 助理配置不能为空");
        }
        application.setAiAssistantConfig(normalizeJsonObject(
                dto.getAiAssistantConfig(), "AI 助理配置", true));
        updateById(application);
        baseMapper.markChanged(resolveTenantId(), id);
    }

    public Map<String, Object> aiAssistantStatus(Long id) {
        BusinessApplicationVO application = detail(id);
        JSONObject config = parseJsonObject(application.getAiAssistantConfig());
        boolean bound = config.get("agentId") != null
                || StringUtils.isNotBlank(config.getString("agentCode"));
        boolean enabled = Boolean.TRUE.equals(config.getBoolean("enabled"));
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("bound", bound);
        status.put("enabled", enabled);
        status.put("published", application.getLastPublishVersion() != null);
        status.put("available", bound && enabled && EnableStatus.ENABLED.matches(application.getStatus())
                && application.getLastPublishVersion() != null);
        status.put("config", config);
        return status;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> distribute(Long id, BusinessApplicationDistributionDTO dto) {
        AiBusinessApplication application = requireEntity(id);
        if (application.getLastPublishVersion() == null) {
            throw new BusinessException("应用尚未发布，不能执行分发");
        }
        if (dto == null) {
            throw new BusinessException("分发配置不能为空");
        }
        String channel = StringUtils.upperCase(StringUtils.trimToEmpty(dto.getChannel()), Locale.ROOT);
        if (!Set.of("WORKBENCH", "DINGTALK").contains(channel)) {
            throw new BusinessException("暂不支持该分发渠道");
        }
        String targetType = StringUtils.upperCase(
                StringUtils.defaultIfBlank(dto.getTargetType(), "CURRENT_USER"), Locale.ROOT);
        if (!Set.of("CURRENT_USER", "ROLES").contains(targetType)) {
            throw new BusinessException("分发目标不正确");
        }
        if (enabledTargetRequiresCurrentUser(targetType) && currentUserId() == null) {
            throw new BusinessException("未获取到当前用户，无法保存工作台分发目标");
        }
        List<Long> roleIds = dto.getRoleIds() == null ? List.of() : dto.getRoleIds().stream()
                .filter(Objects::nonNull).distinct().toList();
        boolean enabled = !Boolean.FALSE.equals(dto.getEnabled());
        if (enabled && "ROLES".equals(targetType) && roleIds.isEmpty()) {
            throw new BusinessException("按角色分发时至少选择一个角色");
        }
        if (enabled && "ROLES".equals(targetType)) {
            assertActiveDistributionRoles(roleIds);
            assertDistributionRoleManagementScope(roleIds);
        }
        if (enabled && "DINGTALK".equals(channel)
                && StringUtils.isBlank(dto.getManagedConnectorKey())) {
            throw new BusinessException("请先在集成中心配置受管钉钉连接器");
        }

        JSONObject portalConfig = parseJsonObject(application.getPortalConfig());
        JSONObject distribution = portalConfig.getJSONObject("distribution");
        if (distribution == null) {
            distribution = new JSONObject();
        }
        JSONObject channelConfig = new JSONObject();
        channelConfig.put("enabled", enabled);
        channelConfig.put("targetType", targetType);
        channelConfig.put("roleIds", roleIds);
        channelConfig.put("targetUserId", "CURRENT_USER".equals(targetType) ? currentUserId() : null);
        channelConfig.put("managedConnectorKey", StringUtils.trimToNull(dto.getManagedConnectorKey()));
        channelConfig.put("status", "DINGTALK".equals(channel)
                ? "PENDING_EXTERNAL_SYNC" : "CONFIGURED");
        distribution.put(channel.toLowerCase(Locale.ROOT), channelConfig);
        portalConfig.put("distribution", distribution);
        application.setPortalConfig(normalizeJsonObject(portalConfig, "门户配置", true));
        updateById(application);
        baseMapper.markChanged(resolveTenantId(), id);
        return new LinkedHashMap<>(channelConfig);
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessApplicationCreateVO create(BusinessApplicationDTO dto) {
        if (dto == null) {
            throw new BusinessException("业务应用不能为空");
        }
        AiBusinessApplication application = new AiBusinessApplication();
        copyDtoToEntity(dto, application, true);
        application.setDesignStatus(BusinessApplicationDesignStatus.DRAFT.getCode());
        save(application);
        return new BusinessApplicationCreateVO(application.getId(), application.getApplicationCode());
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
        // 删除应用时自动停用其下访问入口、禁用管理端菜单，并拆开归属，避免手工逐个停入口
        List<AiBusinessApp> entries = businessAppMapper.selectByApplicationId(tenantId, application.getId());
        for (AiBusinessApp entry : entries) {
            disableEntryManagementMenu(entry);
        }
        businessAppMapper.disableAndDetachByApplicationId(tenantId, application.getId());
        applicationObjectMapper.logicDeleteByApplicationId(tenantId, application.getId());
        removeById(application.getId());
    }

    private void disableEntryManagementMenu(AiBusinessApp entry) {
        if (entry == null || menuRegisterAdapter == null) {
            return;
        }
        Long menuResourceId = readEntryMenuResourceId(entry.getOptions());
        if (menuResourceId != null) {
            menuRegisterAdapter.disableMenu(menuResourceId);
        }
    }

    private Long readEntryMenuResourceId(String optionsJson) {
        JSONObject options = parseJsonObject(optionsJson);
        JSONObject adminMenu = options.getJSONObject("adminMenu");
        Object raw = null;
        if (adminMenu != null) {
            raw = adminMenu.get("menuResourceId");
        }
        if (raw == null) {
            raw = options.get("menuResourceId");
        }
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        String text = StringUtils.trimToNull(String.valueOf(raw));
        if (text == null) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
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
        String portalSlug = StringUtils.trimToNull(text(snapshot.get("portalSlug")));
        if (portalSlug != null) {
            validatePortalSlug(portalSlug);
            assertPortalSlugAvailable(portalSlug, applicationId);
            application.setPortalSlug(portalSlug);
        }
        application.setPortalConfig(normalizeJsonObject(snapshot.get("portalConfig"), "门户配置", true));
        application.setAiAssistantConfig(normalizeJsonObject(
                snapshot.get("aiAssistantConfig"), "AI 助理配置", true));
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
        String applicationName = StringUtils.trimToNull(dto.getApplicationName());
        if (applicationName == null) {
            throw new BusinessException("应用名称不能为空");
        }
        String suiteCode = StringUtils.trimToNull(dto.getSuiteCode());
        suiteService.requireByCode(suiteCode);
        String applicationCode = resolveApplicationCode(
                requestedCode, application, create, suiteCode, applicationName);
        if (!create && !StringUtils.equals(application.getSuiteCode(), suiteCode)) {
            assertSuiteMoveAllowed(application.getId());
        }
        String options = normalizeOptions(dto.getOptions());
        application.setTenantId(resolveTenantId());
        application.setApplicationCode(applicationCode);
        String requestedSlug = StringUtils.trimToNull(dto.getPortalSlug());
        String portalSlug = requestedSlug != null
                ? requestedSlug
                : (create ? defaultPortalSlug(applicationCode) : StringUtils.trimToNull(application.getPortalSlug()));
        if (portalSlug == null) {
            portalSlug = defaultPortalSlug(applicationCode);
        }
        validatePortalSlug(portalSlug);
        assertPortalSlugAvailable(portalSlug, create ? null : application.getId());
        application.setPortalSlug(portalSlug);
        application.setApplicationName(applicationName);
        application.setSuiteCode(suiteCode);
        application.setIcon(StringUtils.trimToNull(dto.getIcon()));
        application.setDescription(StringUtils.trimToNull(dto.getDescription()));
        application.setStatus(normalizeStatus(dto.getStatus()));
        application.setOptions(options);
        String portalConfig = StringUtils.trimToNull(dto.getPortalConfig());
        if (portalConfig != null || create) {
            application.setPortalConfig(normalizeJsonObject(portalConfig, "门户配置", true));
        }
        String aiAssistantConfig = StringUtils.trimToNull(dto.getAiAssistantConfig());
        if (aiAssistantConfig != null || create) {
            application.setAiAssistantConfig(normalizeJsonObject(aiAssistantConfig, "AI 助理配置", true));
        }
    }

    private String resolveApplicationCode(
            String requestedCode,
            AiBusinessApplication application,
            boolean create,
            String suiteCode,
            String applicationName) {
        if (!create) {
            String currentCode = application.getApplicationCode();
            if (requestedCode != null && !StringUtils.equals(requestedCode, currentCode)) {
                throw new BusinessException("应用编码创建后不能修改");
            }
            validateApplicationCode(currentCode);
            assertApplicationCodeAvailable(currentCode, application.getId());
            return currentCode;
        }
        if (requestedCode != null) {
            validateApplicationCode(requestedCode);
            assertApplicationCodeAvailable(requestedCode, null);
            return requestedCode;
        }
        String baseCode = namingService.buildApplicationCode(suiteCode, applicationName);
        validateApplicationCode(baseCode);
        for (int sequence = 1; sequence <= GENERATED_CODE_ATTEMPTS; sequence++) {
            String candidate = sequence == 1 ? baseCode : appendSequence(baseCode, sequence);
            if (baseMapper.countByApplicationCode(resolveTenantId(), candidate, null) == 0) {
                return candidate;
            }
        }
        throw new BusinessException("无法生成唯一应用编码，请在高级设置中填写应用编码");
    }

    private void validateApplicationCode(String applicationCode) {
        if (StringUtils.isBlank(applicationCode) || !CODE_PATTERN.matcher(applicationCode).matches()) {
            throw new BusinessException("应用编码格式不正确（字母开头，仅含字母、数字和下划线，2-64字符）");
        }
    }

    private void assertApplicationCodeAvailable(String applicationCode, Long excludeId) {
        if (baseMapper.countByApplicationCode(resolveTenantId(), applicationCode, excludeId) > 0) {
            throw new BusinessException("应用编码已存在: " + applicationCode);
        }
        if (baseMapper.countByPortalSlug(resolveTenantId(), applicationCode, excludeId) > 0) {
            throw new BusinessException("应用编码与已有门户访问地址冲突: " + applicationCode);
        }
    }

    private void assertPortalSlugAvailable(String portalSlug, Long excludeId) {
        if (baseMapper.countByPortalSlug(resolveTenantId(), portalSlug, excludeId) > 0) {
            throw new BusinessException("门户访问地址已被占用: " + portalSlug);
        }
        if (baseMapper.countByApplicationCode(resolveTenantId(), portalSlug, excludeId) > 0) {
            throw new BusinessException("门户访问地址与已有应用编码冲突: " + portalSlug);
        }
    }

    private String defaultPortalSlug(String applicationCode) {
        String code = StringUtils.trimToEmpty(applicationCode);
        if (code.length() <= PORTAL_SLUG_MAX_LENGTH) {
            return code;
        }
        return StringUtils.left(code, 41) + "_" + StringUtils.left(org.apache.commons.codec.digest.DigestUtils.md5Hex(code), 8);
    }

    private void validatePortalSlug(String portalSlug) {
        if (StringUtils.isBlank(portalSlug) || !PORTAL_SLUG_PATTERN.matcher(portalSlug).matches()) {
            throw new BusinessException("门户访问地址格式不正确（2-50 位字母、数字、中划线或下划线）");
        }
        if (RESERVED_PORTAL_SLUGS.contains(portalSlug.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("门户访问地址是系统保留路径，请更换其它值");
        }
    }

    private String normalizeJsonObject(Object value, String label, boolean defaultEmpty) {
        if (value == null) {
            return defaultEmpty ? "{}" : null;
        }
        try {
            JSONObject object;
            if (value instanceof String textValue) {
                if (StringUtils.isBlank(textValue)) {
                    return defaultEmpty ? "{}" : null;
                }
                object = JSON.parseObject(textValue);
            } else {
                object = JSON.parseObject(JSON.toJSONString(value));
            }
            if (object == null || containsSensitiveKey(object)) {
                throw new BusinessException(label + "不能保存密码、Token、Secret 或 API Key");
            }
            return object.toJSONString();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(label + "必须是合法 JSON 对象");
        }
    }

    private JSONObject parseJsonObject(String value) {
        if (StringUtils.isBlank(value)) {
            return new JSONObject();
        }
        try {
            JSONObject object = JSON.parseObject(value);
            return object == null ? new JSONObject() : object;
        } catch (Exception e) {
            throw new BusinessException("应用 JSON 配置格式不正确");
        }
    }

    private String appendSequence(String baseCode, int sequence) {
        String suffix = "_" + sequence;
        String prefix = StringUtils.left(baseCode, APPLICATION_CODE_MAX_LENGTH - suffix.length())
                .replaceAll("_+$", "");
        return prefix + suffix;
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
        result.setScope(StringUtils.upperCase(StringUtils.defaultIfBlank(result.getScope(), "ALL"), Locale.ROOT));
        if (!Set.of("ALL", "CREATED", "RECENT").contains(result.getScope())) {
            throw new BusinessException("应用分组不正确");
        }
        result.setCreatorId("CREATED".equals(result.getScope()) ? resolveCurrentUserId() : null);
        result.setResolvedApplicationIds("RECENT".equals(result.getScope())
                ? normalizeApplicationIds(result.getApplicationIds()) : null);
        LoginUser loginUser = currentLoginUser();
        result.setCurrentUserId(loginUser == null ? null : loginUser.getUserId());
        result.setCurrentRoleIds(normalizeIds(loginUser == null ? null : loginUser.getRoleIds()));
        result.setCurrentDepartmentIds(normalizeIds(loginUser == null ? null : loginUser.getOrgIds()));
        result.setCurrentAdmin(loginUser != null && loginUser.isAdmin());
        if (!"CREATED".equals(result.getScope()) && !Boolean.TRUE.equals(result.getCurrentAdmin())) {
            List<BusinessApplicationVO> candidates = baseMapper.selectApplicationAccessList(resolveTenantId());
            result.setVisibleApplicationIds(candidates == null ? List.of() : candidates.stream()
                    .filter(application -> canCurrentUserAccessPortal(application.getPortalConfig()))
                    .map(BusinessApplicationVO::getId)
                    .filter(Objects::nonNull)
                    .toList());
        } else {
            result.setVisibleApplicationIds(null);
        }
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

    private List<Long> normalizeApplicationIds(String applicationIds) {
        if (StringUtils.isBlank(applicationIds)) {
            return List.of();
        }
        try {
            return Arrays.stream(applicationIds.split(","))
                    .map(StringUtils::trimToNull)
                    .filter(StringUtils::isNotBlank)
                    .map(Long::valueOf)
                    .distinct()
                    .limit(30)
                    .toList();
        } catch (NumberFormatException e) {
            throw new BusinessException("最近访问应用参数不正确");
        }
    }

    private Long resolveCurrentUserId() {
        try {
            Long userId = SessionHelper.getUserId();
            if (userId == null) {
                throw new BusinessException("未获取到当前用户，无法查询我创建的应用");
            }
            return userId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("未获取到当前用户，无法查询我创建的应用");
        }
    }

    private Long currentUserId() {
        LoginUser loginUser = currentLoginUser();
        return loginUser == null ? null : loginUser.getUserId();
    }

    private LoginUser currentLoginUser() {
        try {
            return SessionHelper.getLoginUser();
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<Long> normalizeIds(List<Long> ids) {
        return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private boolean containsIdentifier(JSONArray values, Long identifier) {
        if (values == null || identifier == null) {
            return false;
        }
        String expected = String.valueOf(identifier);
        return values.stream().anyMatch(value -> expected.equals(String.valueOf(value)));
    }

    private boolean intersects(JSONArray configured, List<Long> actual) {
        if (configured == null || actual == null || actual.isEmpty()) {
            return false;
        }
        Set<String> actualIds = actual.stream().filter(Objects::nonNull)
                .map(String::valueOf).collect(java.util.stream.Collectors.toSet());
        return configured.stream().map(String::valueOf).anyMatch(actualIds::contains);
    }

    public boolean isCurrentUserDistributedToWorkbench(String portalConfig) {
        JSONObject distribution = parseJsonObject(portalConfig).getJSONObject("distribution");
        if (distribution == null) {
            return false;
        }
        Object configured = distribution.get("workbench");
        if (!(configured instanceof JSONObject workbench)
                || !Boolean.TRUE.equals(workbench.getBoolean("enabled"))) {
            return false;
        }
        String targetType = StringUtils.defaultIfBlank(workbench.getString("targetType"), "CURRENT_USER");
        if ("CURRENT_USER".equalsIgnoreCase(targetType)) {
            return String.valueOf(currentUserId()).equals(workbench.getString("targetUserId"));
        }
        return "ROLES".equalsIgnoreCase(targetType)
                && intersects(workbench.getJSONArray("roleIds"), normalizeIds(
                currentLoginUser() == null ? null : currentLoginUser().getRoleIds()));
    }

    private void assertPortalPermissionIdentifiers(String portalConfig) {
        JSONObject permission = parseJsonObject(portalConfig).getJSONObject("permission");
        if (permission == null) {
            return;
        }
        assertActiveTenantUsers("应用管理员", permission.getJSONArray("administrators"));
        assertActiveTenantUsers("可见用户", permission.getJSONArray("userIds"));
        assertActiveTenantRoles("可见角色", permission.getJSONArray("roleIds"));
        assertActiveTenantOrgs("可见部门", permission.getJSONArray("departmentIds"));
    }

    private void assertActiveTenantUsers(String label, JSONArray values) {
        List<Long> ids = toLongIds(values);
        if (ids.isEmpty()) {
            return;
        }
        Long count = baseMapper.countActiveTenantUsers(resolveTenantId(), ids);
        if (count == null || count.longValue() != ids.size()) {
            throw new BusinessException(label + "必须属于当前租户且有效");
        }
    }

    private void assertActiveTenantRoles(String label, JSONArray values) {
        List<Long> ids = toLongIds(values);
        if (ids.isEmpty()) {
            return;
        }
        Long count = baseMapper.countActiveTenantRoles(resolveTenantId(), ids);
        if (count == null || count.longValue() != ids.size()) {
            throw new BusinessException(label + "必须属于当前租户且处于启用状态");
        }
    }

    private void assertActiveTenantOrgs(String label, JSONArray values) {
        List<Long> ids = toLongIds(values);
        if (ids.isEmpty()) {
            return;
        }
        Long count = baseMapper.countActiveTenantOrgs(resolveTenantId(), ids);
        if (count == null || count.longValue() != ids.size()) {
            throw new BusinessException(label + "必须属于当前租户且有效");
        }
    }

    private List<Long> toLongIds(JSONArray values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (Object value : values) {
            if (value == null || StringUtils.isBlank(String.valueOf(value))) {
                continue;
            }
            try {
                ids.add(Long.valueOf(String.valueOf(value)));
            } catch (NumberFormatException error) {
                throw new BusinessException("权限对象标识格式不正确");
            }
        }
        return ids.stream().distinct().toList();
    }

    private boolean enabledTargetRequiresCurrentUser(String targetType) {
        return "CURRENT_USER".equals(targetType);
    }

    private void assertActiveDistributionRoles(List<Long> roleIds) {
        Long validCount = baseMapper.countActiveDistributionRoles(resolveTenantId(), roleIds);
        if (validCount == null || validCount.longValue() != roleIds.size()) {
            throw new BusinessException("分发角色必须属于当前租户、处于启用状态并拥有应用门户访问权限");
        }
    }

    private void assertDistributionRoleManagementScope(List<Long> roleIds) {
        LoginUser loginUser = currentLoginUser();
        if (loginUser == null) {
            throw new BusinessException("未获取到当前用户，无法校验角色管理范围");
        }
        if (loginUser.isAdmin() || loginUser.isTenantAdmin()) {
            return;
        }
        Set<Long> manageableRoleIds = Set.copyOf(normalizeIds(loginUser.getRoleIds()));
        if (!manageableRoleIds.containsAll(roleIds)) {
            throw new BusinessException("分发角色超出当前用户的角色管理范围");
        }
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
