package com.mdframe.forge.plugin.generator.service.lowcode;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfigVersion;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeDomain;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeModel;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeObjectSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePolicySchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePublishDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigVersionMapper;
import com.mdframe.forge.plugin.generator.mapper.AiLowcodeModelMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDdlPreviewVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeVersionVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/**
 * 低代码应用发布、版本和回滚服务。
 */
@Service
public class LowcodePublishService {

    private static final String DEPLOY_SKIP_DDL = "SKIP_DDL";
    private static final String DEPLOY_ONLINE_CREATE_TABLE = "ONLINE_CREATE_TABLE";
    private static final String DDL_PERMISSION = "ai:lowcode:deploy-ddl";
    private static final String GENERAL_DOMAIN_CODE = "general";
    private static final String MOUNT_ADMIN = "ADMIN";
    private static final String MOUNT_MOBILE = "MOBILE";
    private static final String MOUNT_BOTH = "BOTH";

    private final ObjectMapper objectMapper;
    private final AiCrudConfigService configService;
    private final LowcodeAppService appService;
    private final LowcodeDomainService domainService;
    private final LowcodeRuntimeConfigBuilder runtimeConfigBuilder;
    private final LowcodeSchemaValidator schemaValidator;
    private final LowcodeDdlService ddlService;
    private final LowcodePolicyService policyService;
    private final MenuRegisterAdapter menuRegisterAdapter;
    private final AiCrudConfigVersionMapper versionMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessAppMapper businessAppMapper;
    private final AiLowcodeModelMapper lowcodeModelMapper;
    private final LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    /**
     * 领域菜单父级缓存（domainId → menuParentId），发布过程中多次递归查询时避免重复读写 sys_resource。
     */
    private final Map<Long, Long> domainMenuParentIdCache = new ConcurrentHashMap<>();

    public LowcodePublishService(ObjectMapper objectMapper,
                                 AiCrudConfigService configService,
                                 LowcodeAppService appService,
                                 LowcodeDomainService domainService,
                                 LowcodeRuntimeConfigBuilder runtimeConfigBuilder,
                                 LowcodeSchemaValidator schemaValidator,
                                 LowcodeDdlService ddlService,
                                 LowcodePolicyService policyService,
                                 MenuRegisterAdapter menuRegisterAdapter,
                                 AiCrudConfigVersionMapper versionMapper,
                                 BusinessObjectMapper businessObjectMapper,
                                 BusinessAppMapper businessAppMapper,
                                 AiLowcodeModelMapper lowcodeModelMapper,
                                 LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver) {
        this.objectMapper = objectMapper;
        this.configService = configService;
        this.appService = appService;
        this.domainService = domainService;
        this.runtimeConfigBuilder = runtimeConfigBuilder;
        this.schemaValidator = schemaValidator;
        this.ddlService = ddlService;
        this.policyService = policyService;
        this.menuRegisterAdapter = menuRegisterAdapter;
        this.versionMapper = versionMapper;
        this.businessObjectMapper = businessObjectMapper;
        this.businessAppMapper = businessAppMapper;
        this.lowcodeModelMapper = lowcodeModelMapper;
        this.runtimeDataSourceResolver = runtimeDataSourceResolver;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long id, LowcodePublishDTO dto) {
        AiCrudConfig config = appService.requireConfig(id);
        LowcodeModelSchema modelSchema = resolvePublishModel(config, dto);
        PublishDomainContext domainContext = resolvePublishDomainContext(config, modelSchema);
        applyDomainToModelSchema(modelSchema, domainContext, config.getConfigKey());
        LowcodePageSchema pageSchema = resolvePublishPage(config, dto, modelSchema);
        schemaValidator.validatePage(pageSchema, modelSchema);

        // 一次解析运行时数据源上下文，后续所有依赖都复用同一份 context，避免 3-4 次重复查询
        LowcodeRuntimeDataSourceContext runtimeContext = runtimeDataSourceResolver.resolve(modelSchema);
        ensureTableReady(modelSchema, dto, runtimeContext);
        // 去掉列校验（FOLLOW_SYSTEM 策略列已由设计器保证，不必再走 listColumns information_schema 查询）
        policyService.normalizeModelSchema(modelSchema);

        LowcodeRuntimeConfig runtimeConfig = runtimeConfigBuilder.buildRuntimeConfig(config.getConfigKey(), modelSchema, pageSchema);
        applyRuntimeConfig(config, modelSchema, pageSchema, runtimeConfig, runtimeContext);
        applyDomainToConfig(config, domainContext);
        applyMenuConfig(config, dto);
        config.setMountTarget(resolveMountTarget(dto, config));

        // 预先解析菜单父级 ID（主事务内），用于事务提交后异步执行菜单注册
        Long menuParentId = null;
        boolean syncMenu = shouldSyncMenu(dto);
        if (syncMenu && shouldMountAdmin(config.getMountTarget())) {
            applyPublishMenuParent(config, dto, domainContext.domain());
            menuParentId = config.getMenuParentId();
        }

        int versionNo = nextVersionNo(config);
        config.setPublishStatus("PUBLISHED");
        config.setPublishedVersion(versionNo);
        config.setPublishTime(LocalDateTime.now());
        config.setPublishBy(SessionHelper.getUserId());
        configService.updateById(config);
        AiCrudConfigVersion version = createVersion(config, versionNo, "publish",
                dto != null ? dto.getRemark() : null);

        // 菜单注册 + 业务入口同步全部放到事务提交后异步执行，不再阻塞响应
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new LowcodePublishPostEvent(config, dto, domainContext, syncMenu, menuParentId));
        }
        return version.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollback(Long id, Long versionId) {
        rollback(id, versionId, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollback(Long id, Long versionId, boolean syncMenu) {
        AiCrudConfig config = appService.requireConfig(id);
        AiCrudConfigVersion targetVersion = versionMapper.selectVersionById(
                resolveTenantId(config), config.getId(), versionId);
        if (targetVersion == null) {
            throw new BusinessException("版本不存在或不属于当前应用");
        }

        Map<String, Object> snapshot = readSnapshot(targetVersion.getPublishSnapshot());
        LowcodeModelSchema modelSchema = readVersionModel(targetVersion);
        PublishDomainContext domainContext = resolveVersionDomainContext(config, targetVersion, snapshot, modelSchema);
        applyDomainToModelSchema(modelSchema, domainContext, config.getConfigKey());
        LowcodePageSchema pageSchema = readVersionPage(targetVersion);

        // 一次解析运行时数据源上下文，后续所有依赖都复用同一份 context
        LowcodeRuntimeDataSourceContext runtimeContext = runtimeDataSourceResolver.resolve(modelSchema);
        LowcodeRuntimeConfig runtimeConfig = runtimeConfigBuilder.buildRuntimeConfig(config.getConfigKey(), modelSchema, pageSchema);
        applyRuntimeConfig(config, modelSchema, pageSchema, runtimeConfig, runtimeContext);
        applyDomainToConfig(config, domainContext);
        applyVersionRuntimeFields(config, targetVersion, snapshot, runtimeConfig);
        applySnapshotMenuFields(config, snapshot);

        Long menuParentId = null;
        if (syncMenu) {
            if (shouldMountAdmin(config.getMountTarget())) {
                Long parentId = config.getMenuParentId() != null
                        ? config.getMenuParentId()
                        : resolveDomainMenuParentId(resolveDomainForConfig(config));
                if (parentId == null) {
                    parentId = menuRegisterAdapter.resolveDefaultLowcodeParentId();
                }
                config.setMenuParentId(parentId);
                menuParentId = parentId;
            }
            config.setMountTarget(StringUtils.defaultIfBlank(
                    text(snapshot.get("mountTarget")), config.getMountTarget()));
        }

        int versionNo = nextVersionNo(config);
        config.setPublishStatus("PUBLISHED");
        config.setPublishedVersion(versionNo);
        config.setPublishTime(LocalDateTime.now());
        config.setPublishBy(SessionHelper.getUserId());
        configService.updateById(config);
        createVersion(config, versionNo, "rollback", "回滚到版本 " + targetVersion.getVersionNo());

        // 菜单注册 + 业务入口同步放到事务提交后异步执行
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new LowcodePublishPostEvent(config, null, domainContext, syncMenu, menuParentId));
        }
    }

    public List<LowcodeVersionVO> listVersions(Long id) {
        AiCrudConfig config = appService.requireConfig(id);
        return versionMapper.selectByConfigId(resolveTenantId(config), config.getId()).stream()
                .map(this::toVersionVO)
                .toList();
    }

    private LowcodeModelSchema resolvePublishModel(AiCrudConfig config, LowcodePublishDTO dto) {
        if (dto != null && dto.getModelSchema() != null) {
            return dto.getModelSchema();
        }
        return appService.readModelSchema(config);
    }

    private LowcodePageSchema resolvePublishPage(AiCrudConfig config, LowcodePublishDTO dto,
                                                 LowcodeModelSchema modelSchema) {
        if (dto != null && dto.getPageSchema() != null) {
            return dto.getPageSchema();
        }
        if (StringUtils.isNotBlank(config.getPageSchema())) {
            return appService.readPageSchema(config);
        }
        return appService.buildDefaultPageSchema(modelSchema);
    }

    private void ensureTableReady(LowcodeModelSchema modelSchema,
                                  LowcodePublishDTO dto,
                                  LowcodeRuntimeDataSourceContext runtimeContext) {
        String deployMode = dto != null && StringUtils.isNotBlank(dto.getDeployMode())
                ? dto.getDeployMode()
                : DEPLOY_SKIP_DDL;
        if (DEPLOY_ONLINE_CREATE_TABLE.equals(deployMode)) {
            if (!Boolean.TRUE.equals(dto.getConfirmOnlineDdl())) {
                throw new BusinessException("在线建表发布需要二次确认");
            }
            if (!SessionHelper.hasPermission(DDL_PERMISSION)) {
                throw new BusinessException("缺少在线建表发布权限: " + DDL_PERMISSION);
            }
            // 与业务对象「确认同步数据库」同一套策略：加列/建表/兼容扩容在线执行；
            // 高风险变更不阻断已可执行部分，但有残留高风险时仍要求导出人工审核后再发。
            LowcodeDdlPreviewVO preview = ddlService.previewCreateTable(modelSchema);
            if (!Boolean.TRUE.equals(preview.getExecutable())) {
                throw new BusinessException("当前数据库差异不允许在线执行，请导出迁移脚本后人工审核");
            }
            if (preview.getDdlStatements() == null || preview.getDdlStatements().isEmpty()) {
                return;
            }
            int manualReview = preview.getManualReviewDdlCount() == null
                    ? ddlService.countUnsafeDdl(modelSchema)
                    : preview.getManualReviewDdlCount();
            int onlineSafe = preview.getOnlineSafeDdlCount() == null
                    ? Math.max(0, preview.getDdlStatements().size() - manualReview)
                    : preview.getOnlineSafeDdlCount();
            if (onlineSafe <= 0 && manualReview > 0) {
                throw new BusinessException("当前差异均为高风险变更（如改类型族、缩短长度），"
                        + "请导出迁移脚本后人工审核执行");
            }
            ddlService.executeSafeDdlOnly(modelSchema);
            if (manualReview > 0) {
                throw new BusinessException("已同步安全结构变更，但仍有 " + manualReview
                        + " 条高风险 DDL 未执行，请导出脚本人工审核后再发布");
            }
            return;
        }
        // 复用外部已解析的 runtimeContext，避免每个 ddl 校验方法再单独 resolve 一次
        if (!ddlService.tableExists(modelSchema, runtimeContext)) {
            throw new BusinessException("数据表不存在，请先在数据模型页同步表结构");
        }
        if (!ddlService.hasSinglePrimaryKey(modelSchema, runtimeContext)) {
            throw new BusinessException("数据表缺少单字段主键，请先在数据模型页修正表结构");
        }
    }

    private void applyRuntimeConfig(AiCrudConfig config,
                                    LowcodeModelSchema modelSchema,
                                    LowcodePageSchema pageSchema,
                                    LowcodeRuntimeConfig runtimeConfig,
                                    LowcodeRuntimeDataSourceContext runtimeContext) {
        config.setTableName(runtimeConfig.getTableName());
        config.setTableComment(runtimeConfig.getTableComment());
        config.setAppName(StringUtils.defaultIfBlank(config.getAppName(), runtimeConfig.getTableComment()));
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setStatus(EnableStatus.DISABLED.codeAsString());
        config.setLayoutType(runtimeConfig.getLayoutType());
        config.setModelSchema(appService.writeJson(modelSchema, "modelSchema"));
        config.setPageSchema(appService.writeJson(pageSchema, "pageSchema"));
        config.setSearchSchema(runtimeConfig.getSearchSchema());
        config.setColumnsSchema(runtimeConfig.getColumnsSchema());
        config.setEditSchema(runtimeConfig.getEditSchema());
        config.setApiConfig(runtimeConfig.getApiConfig());
        config.setOptions(runtimeConfig.getOptions());
        config.setDictConfig(runtimeConfig.getDictConfig());
        config.setDesensitizeConfig(runtimeConfig.getDesensitizeConfig());
        config.setEncryptConfig(runtimeConfig.getEncryptConfig());
        config.setTransConfig(runtimeConfig.getTransConfig());
        applyRuntimeDatasourceConfig(config, runtimeContext);
    }

    private void applyRuntimeDatasourceConfig(AiCrudConfig config, LowcodeRuntimeDataSourceContext context) {
        config.setRuntimeDatasourceId(context.getDatasourceId());
        config.setRuntimeDatasourceCode(context.getDatasourceCode());
        config.setRuntimeDatasourceSnapshot(context.getSnapshot() == null
                ? null
                : appService.writeJson(context.getSnapshot(), "runtimeDatasourceSnapshot"));
        config.setRuntimeTableName(StringUtils.defaultIfBlank(context.getTableName(), config.getTableName()));
        config.setPrimaryKeyField(context.getPrimaryKey().getField());
        config.setPrimaryKeyColumn(context.getPrimaryKey().getColumnName());
        config.setPrimaryKeyType(context.getPrimaryKey().getDataType());
        config.setTenantStrategy(appService.writeJson(context.getTenantStrategy(), "tenantStrategy"));
        config.setAuditStrategy(appService.writeJson(context.getAuditStrategy(), "auditStrategy"));
        config.setLogicDeleteStrategy(appService.writeJson(context.getLogicDeleteStrategy(), "logicDeleteStrategy"));
    }

    private void applyMenuConfig(AiCrudConfig config, LowcodePublishDTO dto) {
        if (dto == null) {
            return;
        }
        if (StringUtils.isNotBlank(dto.getMenuName())) {
            config.setMenuName(dto.getMenuName());
        }
        if (dto.getMenuSort() != null) {
            config.setMenuSort(dto.getMenuSort());
        }
        if (StringUtils.isNotBlank(dto.getMountTarget())) {
            config.setMountTarget(dto.getMountTarget().toUpperCase(Locale.ROOT));
        }
    }

    /**
     * 解析菜单挂载位置：优先使用发布请求中的值，其次使用配置已有值，默认 ADMIN。
     */
    private String resolveMountTarget(LowcodePublishDTO dto, AiCrudConfig config) {
        String fromDto = dto != null ? StringUtils.trimToNull(dto.getMountTarget()) : null;
        if (fromDto != null) {
            return fromDto.toUpperCase(Locale.ROOT);
        }
        return StringUtils.defaultIfBlank(config.getMountTarget(), MOUNT_ADMIN);
    }

    private boolean shouldMountAdmin(String mountTarget) {
        return MOUNT_ADMIN.equalsIgnoreCase(mountTarget) || MOUNT_BOTH.equalsIgnoreCase(mountTarget);
    }

    private boolean shouldMountMobile(String mountTarget) {
        return MOUNT_MOBILE.equalsIgnoreCase(mountTarget) || MOUNT_BOTH.equalsIgnoreCase(mountTarget);
    }

    private Long readMobileMenuResourceId(AiCrudConfig config) {
        String options = config.getOptions();
        if (StringUtils.isBlank(options)) {
            return null;
        }
        try {
            JSONObject obj = JSONObject.parseObject(options);
            Long value = obj.getLong("mobileMenuResourceId");
            return value;
        } catch (Exception e) {
            return null;
        }
    }

    private void writeMobileMenuResourceId(AiCrudConfig config, Long mobileMenuResourceId) {
        JSONObject obj;
        try {
            obj = StringUtils.isNotBlank(config.getOptions())
                    ? JSONObject.parseObject(config.getOptions()) : new JSONObject();
        } catch (Exception e) {
            obj = new JSONObject();
        }
        if (mobileMenuResourceId != null) {
            obj.put("mobileMenuResourceId", mobileMenuResourceId);
        } else {
            obj.remove("mobileMenuResourceId");
        }
        config.setOptions(obj.toJSONString());
    }

    /**
     * 异步后置菜单注册入口：支持管理端（ADMIN）、移动端（MOBILE）或两端同时（BOTH）。
     * 菜单父级 ID 已由主事务预先解析传入，避免在异步上下文中重复解析。
     */
    void registerOrUpdateMenuAsync(AiCrudConfig config, boolean syncMenu, Long resolvedParentId) {
        String mountTarget = StringUtils.defaultIfBlank(config.getMountTarget(), MOUNT_ADMIN);
        boolean mountAdmin = shouldMountAdmin(mountTarget);
        boolean mountMobile = shouldMountMobile(mountTarget);
        String menuName = StringUtils.defaultIfBlank(config.getMenuName(),
                StringUtils.defaultIfBlank(config.getAppName(), config.getTableComment()));
        Integer sort = config.getMenuSort() != null ? config.getMenuSort() : 0;

        if (!syncMenu) {
            if (config.getMenuResourceId() != null) {
                menuRegisterAdapter.disableMenu(config.getMenuResourceId());
            }
            Long mobileResourceId = readMobileMenuResourceId(config);
            if (mobileResourceId != null) {
                menuRegisterAdapter.disableMenu(mobileResourceId);
            }
            return;
        }

        // 管理端菜单注册
        if (mountAdmin) {
            Long parentId = resolvedParentId != null ? resolvedParentId : menuRegisterAdapter.resolveDefaultLowcodeParentId();
            if (config.getMenuResourceId() == null) {
                Long menuResourceId = menuRegisterAdapter.registerMenu(menuName, parentId, config.getConfigKey(), sort);
                config.setMenuResourceId(menuResourceId);
            } else {
                menuRegisterAdapter.updateMenu(config.getMenuResourceId(), menuName, parentId, sort);
            }
            config.setMenuName(menuName);
            config.setMenuParentId(parentId);
            config.setMenuSort(sort);
        } else {
            // 不挂管理端时禁用已有管理端菜单
            if (config.getMenuResourceId() != null) {
                menuRegisterAdapter.disableMenu(config.getMenuResourceId());
            }
        }

        // 移动端菜单注册
        if (mountMobile) {
            String mobilePath = "/pages/lowcode-runtime?configKey=" + config.getConfigKey();
            String mobilePerms = "ai:crud:h5:" + config.getConfigKey();
            Long existingMobileId = readMobileMenuResourceId(config);
            if (existingMobileId == null) {
                Long mobileMenuId = menuRegisterAdapter.registerAppMenu(
                        menuName, 0L, mobilePath, mobilePath,
                        mobilePerms, null, sort, true, "h5");
                writeMobileMenuResourceId(config, mobileMenuId);
            } else {
                menuRegisterAdapter.updateAppMenu(
                        existingMobileId, menuName, 0L, mobilePath, mobilePath,
                        mobilePerms, null, sort, true, "h5");
            }
        } else {
            // 不挂移动端时禁用已有移动端菜单
            Long mobileResourceId = readMobileMenuResourceId(config);
            if (mobileResourceId != null) {
                menuRegisterAdapter.disableMenu(mobileResourceId);
            }
        }
    }

    private boolean shouldSyncMenu(LowcodePublishDTO dto) {
        return dto == null || !Boolean.FALSE.equals(dto.getSyncMenu());
    }

    private void disablePublishedMenu(AiCrudConfig config) {
        if (config != null && config.getMenuResourceId() != null) {
            menuRegisterAdapter.disableMenu(config.getMenuResourceId());
        }
    }

    void syncBusinessRuntimeEntry(AiCrudConfig config, LowcodePublishDTO dto, PublishDomainContext context) {
        if (config == null || context == null || context.domain() == null || StringUtils.isBlank(config.getConfigKey())) {
            return;
        }
        Long tenantId = resolveTenantId(config);
        AiBusinessApp existingApp = businessAppMapper.selectByConfigKey(tenantId, config.getConfigKey());
        String suiteCode = StringUtils.firstNonBlank(dto != null ? dto.getBusinessSuiteCode() : null,
                existingApp == null ? null : existingApp.getSuiteCode(),
                context.domain().getDomainCode());
        String businessObjectCode = StringUtils.firstNonBlank(dto != null ? dto.getBusinessObjectCode() : null,
                existingApp == null ? null : existingApp.getObjectCode(),
                config.getObjectCode(),
                context.objectCode());
        if (StringUtils.isBlank(suiteCode) || StringUtils.isBlank(businessObjectCode)) {
            return;
        }
        AiBusinessObject businessObject = findBusinessObject(tenantId, suiteCode, businessObjectCode);
        if (businessObject == null) {
            return;
        }

        AiLowcodeModel model = StringUtils.isBlank(config.getObjectCode())
                ? null
                : lowcodeModelMapper.selectByCode(tenantId, context.domain().getId(), config.getObjectCode());
        businessObject.setModelId(model == null ? businessObject.getModelId() : model.getId());
        businessObject.setModelCode(StringUtils.defaultIfBlank(config.getObjectCode(), businessObject.getModelCode()));
        businessObjectMapper.updateById(businessObject);

        AiBusinessApp app = existingApp;
        if (app == null) {
            app = businessAppMapper.selectRuntimeAppByObject(tenantId, suiteCode, businessObject.getObjectCode());
        }
        boolean create = app == null;
        if (create) {
            app = new AiBusinessApp();
            app.setTenantId(tenantId);
            app.setAppCode(resolveRuntimeAppCode(tenantId, suiteCode, businessObject.getObjectCode(), config));
            app.setAppName(resolveRuntimeAppName(config, dto, businessObject));
            app.setAppType("BUSINESS");
            app.setEntryMode("RUNTIME");
            app.setEntryUrl(resolveEntryUrl(config));
            app.setIcon(StringUtils.defaultIfBlank(businessObject.getIcon(), "ionicons5:AppsOutline"));
            app.setDescription(StringUtils.defaultIfBlank(config.getTableComment(), "低代码发布生成的标准业务应用入口"));
            app.setStatus(EnableStatus.ENABLED.getCode());
            app.setSortOrder(config.getMenuSort() == null ? 0 : config.getMenuSort());
            app.setOptions("{\"source\":\"lowcode_publish\"}");
        }
        // 对象发布只补齐入口的运行绑定。已有入口的挂载端、菜单、展示和状态属于应用级配置，
        // 不能再用自动生成的管理端默认值覆盖，否则移动端入口会在重新发布后退回管理端。
        app.setSuiteCode(suiteCode);
        app.setObjectCode(businessObject.getObjectCode());
        app.setConfigKey(config.getConfigKey());
        if (create) {
            businessAppMapper.insert(app);
        } else {
            businessAppMapper.updateById(app);
        }
    }

    private AiBusinessObject findBusinessObject(Long tenantId, String suiteCode, String objectCode) {
        // 对象编码统一小写存储，去掉多余的大写兜底查询
        return businessObjectMapper.selectByObjectCode(tenantId, suiteCode, objectCode);
    }

    private String resolveRuntimeAppName(AiCrudConfig config, LowcodePublishDTO dto, AiBusinessObject businessObject) {
        return StringUtils.firstNonBlank(
                config.getMenuName(),
                config.getAppName(),
                dto != null ? dto.getBusinessObjectName() : null,
                businessObject.getObjectName(),
                config.getConfigKey()
        );
    }

    private String resolveRuntimeAppCode(Long tenantId, String suiteCode, String objectCode, AiCrudConfig config) {
        String base = normalizeAppCode(suiteCode + "_" + objectCode + "_RUNTIME");
        if (businessAppMapper.countByAppCode(tenantId, base, null) == 0) {
            return base;
        }
        String fallback = normalizeAppCode(base + "_" + config.getId());
        if (businessAppMapper.countByAppCode(tenantId, fallback, null) == 0) {
            return fallback;
        }
        return normalizeAppCode(base + "_" + System.currentTimeMillis());
    }

    private String normalizeAppCode(String value) {
        String normalized = StringUtils.defaultString(value)
                .replaceAll("[^A-Za-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .toUpperCase(Locale.ROOT)
                .replaceAll("^[^A-Z]+", "")
                .replaceAll("_+$", "");
        if (StringUtils.isBlank(normalized)) {
            normalized = "LOWCODE_RUNTIME_APP";
        }
        return normalized.length() > 64 ? normalized.substring(0, 64).replaceAll("_+$", "") : normalized;
    }

    private int nextVersionNo(AiCrudConfig config) {
        Integer maxVersionNo = versionMapper.selectMaxVersionNo(resolveTenantId(config), config.getId());
        return (maxVersionNo == null ? 0 : maxVersionNo) + 1;
    }

    private AiCrudConfigVersion createVersion(AiCrudConfig config, Integer versionNo, String versionType, String remark) {
        AiCrudConfigVersion version = new AiCrudConfigVersion();
        version.setTenantId(resolveTenantId(config));
        version.setConfigId(config.getId());
        version.setConfigKey(config.getConfigKey());
        version.setDomainId(config.getDomainId());
        version.setDomainCode(config.getDomainCode());
        version.setObjectCode(config.getObjectCode());
        version.setObjectName(config.getObjectName());
        version.setVersionNo(versionNo);
        version.setVersionType(versionType);
        version.setModelSchema(config.getModelSchema());
        version.setPageSchema(config.getPageSchema());
        version.setSearchSchema(config.getSearchSchema());
        version.setColumnsSchema(config.getColumnsSchema());
        version.setEditSchema(config.getEditSchema());
        version.setApiConfig(config.getApiConfig());
        version.setOptions(config.getOptions());
        version.setRuntimeDatasourceId(config.getRuntimeDatasourceId());
        version.setRuntimeDatasourceCode(config.getRuntimeDatasourceCode());
        version.setRuntimeDatasourceSnapshot(config.getRuntimeDatasourceSnapshot());
        version.setRuntimeTableName(config.getRuntimeTableName());
        version.setPrimaryKeyField(config.getPrimaryKeyField());
        version.setPrimaryKeyColumn(config.getPrimaryKeyColumn());
        version.setPrimaryKeyType(config.getPrimaryKeyType());
        version.setTenantStrategy(config.getTenantStrategy());
        version.setAuditStrategy(config.getAuditStrategy());
        version.setLogicDeleteStrategy(config.getLogicDeleteStrategy());
        version.setPublishSnapshot(writeSnapshot(config));
        version.setRemark(StringUtils.defaultIfBlank(remark, "发布低代码应用"));
        versionMapper.insert(version);
        return version;
    }

    private void applyVersionRuntimeFields(AiCrudConfig config,
                                           AiCrudConfigVersion version,
                                           Map<String, Object> snapshot,
                                           LowcodeRuntimeConfig fallback) {
        config.setSearchSchema(StringUtils.defaultIfBlank(version.getSearchSchema(), fallback.getSearchSchema()));
        config.setColumnsSchema(StringUtils.defaultIfBlank(version.getColumnsSchema(), fallback.getColumnsSchema()));
        config.setEditSchema(StringUtils.defaultIfBlank(version.getEditSchema(), fallback.getEditSchema()));
        config.setApiConfig(StringUtils.defaultIfBlank(version.getApiConfig(), fallback.getApiConfig()));
        config.setOptions(StringUtils.defaultIfBlank(version.getOptions(), fallback.getOptions()));
        config.setDictConfig(StringUtils.defaultIfBlank(text(snapshot.get("dictConfig")), fallback.getDictConfig()));
        config.setDesensitizeConfig(StringUtils.defaultIfBlank(text(snapshot.get("desensitizeConfig")), fallback.getDesensitizeConfig()));
        config.setEncryptConfig(StringUtils.defaultIfBlank(text(snapshot.get("encryptConfig")), fallback.getEncryptConfig()));
        config.setTransConfig(StringUtils.defaultIfBlank(text(snapshot.get("transConfig")), fallback.getTransConfig()));
        config.setLayoutType(StringUtils.defaultIfBlank(text(snapshot.get("layoutType")), fallback.getLayoutType()));
        config.setTableName(StringUtils.defaultIfBlank(text(snapshot.get("tableName")), fallback.getTableName()));
        config.setTableComment(StringUtils.defaultIfBlank(text(snapshot.get("tableComment")), fallback.getTableComment()));
        config.setAppName(StringUtils.defaultIfBlank(text(snapshot.get("appName")), config.getTableComment()));
        if (version.getRuntimeDatasourceId() != null) {
            config.setRuntimeDatasourceId(version.getRuntimeDatasourceId());
        }
        config.setRuntimeDatasourceCode(StringUtils.defaultIfBlank(
                version.getRuntimeDatasourceCode(), config.getRuntimeDatasourceCode()));
        config.setRuntimeDatasourceSnapshot(StringUtils.defaultIfBlank(
                version.getRuntimeDatasourceSnapshot(), config.getRuntimeDatasourceSnapshot()));
        config.setRuntimeTableName(StringUtils.defaultIfBlank(
                version.getRuntimeTableName(), config.getRuntimeTableName()));
        config.setPrimaryKeyField(StringUtils.defaultIfBlank(
                version.getPrimaryKeyField(), config.getPrimaryKeyField()));
        config.setPrimaryKeyColumn(StringUtils.defaultIfBlank(
                version.getPrimaryKeyColumn(), config.getPrimaryKeyColumn()));
        config.setPrimaryKeyType(StringUtils.defaultIfBlank(
                version.getPrimaryKeyType(), config.getPrimaryKeyType()));
        config.setTenantStrategy(StringUtils.defaultIfBlank(
                version.getTenantStrategy(), config.getTenantStrategy()));
        config.setAuditStrategy(StringUtils.defaultIfBlank(
                version.getAuditStrategy(), config.getAuditStrategy()));
        config.setLogicDeleteStrategy(StringUtils.defaultIfBlank(
                version.getLogicDeleteStrategy(), config.getLogicDeleteStrategy()));
    }

    private void applySnapshotMenuFields(AiCrudConfig config, Map<String, Object> snapshot) {
        config.setMenuName(StringUtils.defaultIfBlank(text(snapshot.get("menuName")),
                StringUtils.defaultIfBlank(config.getAppName(), config.getTableComment())));
        config.setMenuParentId(numberAsLong(snapshot.get("menuParentId"), config.getMenuParentId()));
        config.setMenuSort(numberAsInteger(snapshot.get("menuSort"), config.getMenuSort()));
        config.setMountTarget(StringUtils.defaultIfBlank(
                text(snapshot.get("mountTarget")), config.getMountTarget()));
    }

    private String resolveEntryUrl(AiCrudConfig config) {
        String mountTarget = StringUtils.defaultIfBlank(config.getMountTarget(), MOUNT_ADMIN);
        if (shouldMountMobile(mountTarget) && !shouldMountAdmin(mountTarget)) {
            return "/pages/lowcode-runtime?configKey=" + config.getConfigKey();
        }
        return "/ai/crud-page/" + config.getConfigKey();
    }

    private LowcodeModelSchema readVersionModel(AiCrudConfigVersion version) {
        return readJson(version.getModelSchema(), LowcodeModelSchema.class, "版本modelSchema");
    }

    private LowcodePageSchema readVersionPage(AiCrudConfigVersion version) {
        return readJson(version.getPageSchema(), LowcodePageSchema.class, "版本pageSchema");
    }

    private String writeSnapshot(AiCrudConfig config) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("configKey", config.getConfigKey());
        snapshot.put("tableName", config.getTableName());
        snapshot.put("tableComment", config.getTableComment());
        snapshot.put("appName", config.getAppName());
        snapshot.put("layoutType", config.getLayoutType());
        snapshot.put("domainId", config.getDomainId());
        snapshot.put("domainCode", config.getDomainCode());
        snapshot.put("objectCode", config.getObjectCode());
        snapshot.put("objectName", config.getObjectName());
        snapshot.put("domain", buildDomainSnapshot(config));
        snapshot.put("object", buildObjectSnapshot(config));
        snapshot.put("dictConfig", config.getDictConfig());
        snapshot.put("desensitizeConfig", config.getDesensitizeConfig());
        snapshot.put("encryptConfig", config.getEncryptConfig());
        snapshot.put("transConfig", config.getTransConfig());
        snapshot.put("runtimeDatasourceId", config.getRuntimeDatasourceId());
        snapshot.put("runtimeDatasourceCode", config.getRuntimeDatasourceCode());
        snapshot.put("runtimeDatasourceSnapshot", config.getRuntimeDatasourceSnapshot());
        snapshot.put("runtimeTableName", config.getRuntimeTableName());
        snapshot.put("primaryKeyField", config.getPrimaryKeyField());
        snapshot.put("primaryKeyColumn", config.getPrimaryKeyColumn());
        snapshot.put("primaryKeyType", config.getPrimaryKeyType());
        snapshot.put("tenantStrategy", config.getTenantStrategy());
        snapshot.put("auditStrategy", config.getAuditStrategy());
        snapshot.put("logicDeleteStrategy", config.getLogicDeleteStrategy());
        snapshot.put("menuName", config.getMenuName());
        snapshot.put("menuParentId", config.getMenuParentId());
        snapshot.put("menuSort", config.getMenuSort());
        snapshot.put("menuResourceId", config.getMenuResourceId());
        snapshot.put("mountTarget", config.getMountTarget());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new BusinessException("发布快照生成失败");
        }
    }

    private Map<String, Object> readSnapshot(String snapshotJson) {
        if (StringUtils.isBlank(snapshotJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(snapshotJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new BusinessException("版本快照格式不正确");
        }
    }

    private <T> T readJson(String json, Class<T> type, String fieldName) {
        if (StringUtils.isBlank(json)) {
            throw new BusinessException(fieldName + "不能为空");
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new BusinessException(fieldName + "格式不正确");
        }
    }

    private LowcodeVersionVO toVersionVO(AiCrudConfigVersion version) {
        LowcodeVersionVO vo = new LowcodeVersionVO();
        vo.setId(version.getId());
        vo.setConfigId(version.getConfigId());
        vo.setConfigKey(version.getConfigKey());
        vo.setDomainId(version.getDomainId());
        vo.setDomainCode(version.getDomainCode());
        vo.setObjectCode(version.getObjectCode());
        vo.setObjectName(version.getObjectName());
        vo.setVersionNo(version.getVersionNo());
        vo.setVersionType(version.getVersionType());
        vo.setRemark(version.getRemark());
        vo.setCreateTime(version.getCreateTime());
        vo.setCreateBy(version.getCreateBy());
        return vo;
    }

    private Long resolveTenantId(AiCrudConfig config) {
        if (config.getTenantId() != null) {
            return config.getTenantId();
        }
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }

    private PublishDomainContext resolvePublishDomainContext(AiCrudConfig config, LowcodeModelSchema modelSchema) {
        LowcodeDomainRef schemaDomain = modelSchema != null ? modelSchema.getDomain() : null;
        LowcodeObjectSchema schemaObject = modelSchema != null ? modelSchema.getObject() : null;
        Long domainId = firstNonNull(config.getDomainId(), schemaDomain != null ? schemaDomain.getId() : null);
        String domainCode = StringUtils.firstNonBlank(config.getDomainCode(), schemaDomain != null ? schemaDomain.getCode() : null);
        AiLowcodeDomain domain = resolveDomain(domainId, domainCode);
        String objectCode = StringUtils.firstNonBlank(config.getObjectCode(),
                schemaObject != null ? schemaObject.getCode() : null,
                config.getConfigKey(),
                modelSchema != null ? modelSchema.getTableName() : null);
        String objectName = StringUtils.firstNonBlank(config.getObjectName(),
                schemaObject != null ? schemaObject.getName() : null,
                modelSchema != null ? modelSchema.getBusinessName() : null,
                config.getAppName(),
                config.getTableComment(),
                objectCode);
        return new PublishDomainContext(domain, objectCode, objectName);
    }

    private PublishDomainContext resolveVersionDomainContext(AiCrudConfig config,
                                                             AiCrudConfigVersion version,
                                                             Map<String, Object> snapshot,
                                                             LowcodeModelSchema modelSchema) {
        LowcodeDomainRef schemaDomain = modelSchema != null ? modelSchema.getDomain() : null;
        LowcodeObjectSchema schemaObject = modelSchema != null ? modelSchema.getObject() : null;
        Long domainId = firstNonNull(
                version.getDomainId(),
                numberAsLong(snapshot.get("domainId"), null),
                schemaDomain != null ? schemaDomain.getId() : null,
                config.getDomainId());
        String domainCode = StringUtils.firstNonBlank(
                version.getDomainCode(),
                text(snapshot.get("domainCode")),
                schemaDomain != null ? schemaDomain.getCode() : null,
                config.getDomainCode());
        AiLowcodeDomain domain = resolveDomain(domainId, domainCode);
        String objectCode = StringUtils.firstNonBlank(
                version.getObjectCode(),
                text(snapshot.get("objectCode")),
                schemaObject != null ? schemaObject.getCode() : null,
                config.getObjectCode(),
                config.getConfigKey());
        String objectName = StringUtils.firstNonBlank(
                version.getObjectName(),
                text(snapshot.get("objectName")),
                schemaObject != null ? schemaObject.getName() : null,
                config.getObjectName(),
                modelSchema != null ? modelSchema.getBusinessName() : null,
                config.getAppName(),
                config.getTableComment(),
                objectCode);
        return new PublishDomainContext(domain, objectCode, objectName);
    }

    private AiLowcodeDomain resolveDomainForConfig(AiCrudConfig config) {
        return resolveDomain(config.getDomainId(), config.getDomainCode());
    }

    private AiLowcodeDomain resolveDomain(Long domainId, String domainCode) {
        if (domainId != null) {
            try {
                return domainService.requireDomain(domainId);
            } catch (BusinessException ignored) {
                // 旧版本快照可能只保留编码，继续按编码和通用业务域兜底。
            }
        }
        if (StringUtils.isNotBlank(domainCode)) {
            AiLowcodeDomain domain = domainService.getByCode(domainCode);
            if (domain != null) {
                return domain;
            }
        }
        AiLowcodeDomain generalDomain = domainService.getByCode(GENERAL_DOMAIN_CODE);
        if (generalDomain == null) {
            throw new BusinessException("通用业务域不存在，请先执行低代码业务领域迁移脚本");
        }
        return generalDomain;
    }

    private void applyDomainToModelSchema(LowcodeModelSchema modelSchema, PublishDomainContext context, String configKey) {
        if (modelSchema == null || context == null || context.domain() == null) {
            return;
        }
        normalizeModelCollections(modelSchema);
        modelSchema.setSchemaVersion(2);

        LowcodeDomainRef domainRef = modelSchema.getDomain() == null ? new LowcodeDomainRef() : modelSchema.getDomain();
        domainRef.setId(context.domain().getId());
        domainRef.setCode(context.domain().getDomainCode());
        domainRef.setName(context.domain().getDomainName());
        modelSchema.setDomain(domainRef);

        LowcodeObjectSchema object = modelSchema.getObject() == null ? new LowcodeObjectSchema() : modelSchema.getObject();
        object.setCode(context.objectCode());
        object.setName(context.objectName());
        if (StringUtils.isBlank(object.getDescription())) {
            object.setDescription(StringUtils.defaultIfBlank(modelSchema.getBusinessName(), context.objectName()));
        }
        modelSchema.setObject(object);
        ensureTableName(modelSchema, context.domain(), context.objectCode(), configKey);
        policyService.normalizeModelSchema(modelSchema);
    }

    private void ensureTableName(LowcodeModelSchema modelSchema, AiLowcodeDomain domain, String objectCode, String configKey) {
        if (schemaValidator.isValidTableName(modelSchema.getTableName())) {
            return;
        }
        String prefix = StringUtils.defaultIfBlank(domain.getTablePrefix(), "biz_");
        String base = StringUtils.firstNonBlank(
                objectCode,
                modelSchema.getObject() == null ? null : modelSchema.getObject().getCode(),
                configKey,
                "runtime_model");
        modelSchema.setTableName(normalizeTableName(prefix + base));
    }

    private String normalizeTableName(String value) {
        String normalized = StringUtils.defaultString(value)
                .trim()
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .toLowerCase(Locale.ROOT)
                .replaceAll("^[^a-z]+", "")
                .replaceAll("_+$", "");
        if (StringUtils.isBlank(normalized)) {
            normalized = "biz_lowcode_model";
        }
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64).replaceAll("_+$", "");
        }
        return StringUtils.defaultIfBlank(normalized, "biz_lowcode_model");
    }

    private void normalizeModelCollections(LowcodeModelSchema modelSchema) {
        if (modelSchema.getFields() == null) {
            modelSchema.setFields(new ArrayList<>());
        }
        if (modelSchema.getRelations() == null) {
            modelSchema.setRelations(new ArrayList<>());
        }
        if (modelSchema.getPolicies() == null) {
            modelSchema.setPolicies(new LowcodePolicySchema());
        }
        if (modelSchema.getChildren() == null) {
            modelSchema.setChildren(new ArrayList<>());
        }
    }

    private void applyDomainToConfig(AiCrudConfig config, PublishDomainContext context) {
        if (context == null || context.domain() == null) {
            return;
        }
        config.setDomainId(context.domain().getId());
        config.setDomainCode(context.domain().getDomainCode());
        config.setObjectCode(context.objectCode());
        config.setObjectName(context.objectName());
    }

    private void applyPublishMenuParent(AiCrudConfig config, LowcodePublishDTO dto, AiLowcodeDomain domain) {
        Long domainParentId = resolveDomainMenuParentId(domain);
        Long requestParentId = dto == null ? null : dto.getMenuParentId();
        if (requestParentId != null && !requestParentId.equals(domainParentId)) {
            config.setMenuParentId(requestParentId);
            return;
        }
        config.setMenuParentId(domainParentId != null ? domainParentId : menuRegisterAdapter.resolveDefaultLowcodeParentId());
    }

    private Long resolveDomainMenuParentId(AiLowcodeDomain domain) {
        if (domain == null || domain.getId() == null) {
            return resolveDomainMenuParentIdUncached(domain, new HashSet<>());
        }
        return domainMenuParentIdCache.computeIfAbsent(domain.getId(),
                key -> resolveDomainMenuParentIdUncached(domain, new HashSet<>()));
    }

    private Long resolveDomainMenuParentIdUncached(AiLowcodeDomain domain, Set<Long> resolvingDomainIds) {
        if (domain == null) {
            return null;
        }
        Long domainId = domain.getId();
        if (domainId != null && !resolvingDomainIds.add(domainId)) {
            throw new BusinessException("业务领域层级存在循环引用，请先调整领域父级");
        }
        try {
            Long parentMenuId = menuRegisterAdapter.resolveDefaultLowcodeParentId();
            Long parentDomainId = domain.getParentId();
            if (parentDomainId != null && parentDomainId > 0) {
                AiLowcodeDomain parentDomain = domainService.requireDomain(parentDomainId);
                Long resolvedParentMenuId = resolveDomainMenuParentIdUncached(parentDomain, resolvingDomainIds);
                if (resolvedParentMenuId != null) {
                    parentMenuId = resolvedParentMenuId;
                }
            }
            Long menuParentId = menuRegisterAdapter.resolveOrCreateDomainParentId(
                    domain.getDomainCode(), domain.getDomainName(), domain.getSort(), parentMenuId);
            return menuParentId;
        } finally {
            if (domainId != null) {
                resolvingDomainIds.remove(domainId);
            }
        }
    }

    private Map<String, Object> buildDomainSnapshot(AiCrudConfig config) {
        Map<String, Object> domain = new LinkedHashMap<>();
        domain.put("id", config.getDomainId());
        domain.put("code", config.getDomainCode());
        return domain;
    }

    private Map<String, Object> buildObjectSnapshot(AiCrudConfig config) {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("code", config.getObjectCode());
        object.put("name", config.getObjectName());
        return object;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long numberAsLong(Object value, Long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            return Long.valueOf(text);
        }
        return defaultValue;
    }

    private Integer numberAsInteger(Object value, Integer defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            return Integer.valueOf(text);
        }
        return defaultValue;
    }

    record PublishDomainContext(AiLowcodeDomain domain, String objectCode, String objectName) {
    }
}
