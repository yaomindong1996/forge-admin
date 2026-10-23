package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessPublishCheckLevel;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlService;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPublishCheckItemVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDdlPreviewVO;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 应用入口、数据源与表结构部署就绪校验链。 */
final class BusinessObjectDeploymentPublishValidator {

    private static final String DDL_PERMISSION = "ai:lowcode:deploy-ddl";
    private static final Set<String> RUNTIME_OPEN_MODES = Set.of("LIST", "CREATE_FORM", "DETAIL");
    private static final Set<String> APP_ENTRY_TYPES = Set.of(
            "OBJECT_LIST", "CREATE_FORM", "DETAIL_PAGE", "APPROVAL_TODO", "REPORT_DASHBOARD", "EXTERNAL_OR_API");

    private final ObjectMapper objectMapper;
    private final BusinessAppMapper businessAppMapper;
    private final LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver;
    private final LowcodeDdlService ddlService;

    BusinessObjectDeploymentPublishValidator(ObjectMapper objectMapper,
                                             BusinessAppMapper businessAppMapper,
                                             LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver,
                                             LowcodeDdlService ddlService) {
        this.objectMapper = objectMapper;
        this.businessAppMapper = businessAppMapper;
        this.runtimeDataSourceResolver = runtimeDataSourceResolver;
        this.ddlService = ddlService;
    }

    void validateEntry(BusinessObjectDesignerService.DesignerContext context,
                       List<BusinessPublishCheckItemVO> items) {
        checkAppEntry(context, items);
    }

    void validateStorage(LowcodeModelSchema modelSchema, List<BusinessPublishCheckItemVO> items) {
        checkRuntimeDataSource(modelSchema, items);
        checkTable(modelSchema, items);
    }

    private void checkAppEntry(BusinessObjectDesignerService.DesignerContext context,
                               List<BusinessPublishCheckItemVO> items) {
        Long tenantId = resolveTenantId(context);
        AiBusinessObject object = context.getObject();
        AiBusinessApp app = businessAppMapper.selectRuntimeAppByObject(
                tenantId, object.getSuiteCode(), object.getObjectCode());
        if (app == null) {
            add(items, "APP_ENTRY_MISSING", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "应用入口未创建", "发布后需要配置业务应用入口和菜单挂载，否则用户无法从菜单进入填报页", null, null,
                    "CONFIG_APP_ENTRY", "配置入口", "publish", 320);
            return;
        }
        if (EnableStatus.DISABLED.matches(app.getStatus())) {
            add(items, "APP_ENTRY_DISABLED", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "应用入口已停用", "当前业务应用入口已停用，菜单点击后不会进入运行态", null, null,
                    "ENABLE_APP_ENTRY", "启用入口", "publish", 321);
        }
        if (!"RUNTIME".equalsIgnoreCase(StringUtils.defaultString(app.getEntryMode()))) {
            add(items, "APP_ENTRY_MODE_INVALID", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "入口打开模式不是运行态", "业务单据挂载建议使用 RUNTIME 模式，直接打开填报/列表页面", null, null,
                    "CONFIG_APP_ENTRY", "调整入口", "publish", 322);
        }
        if (StringUtils.isBlank(app.getConfigKey())) {
            add(items, "APP_ENTRY_CONFIG_EMPTY", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "入口缺少运行配置", "应用入口没有绑定 configKey，无法稳定打开动态填报页面", null, null,
                    "CONFIG_APP_ENTRY", "绑定运行配置", "publish", 323);
        }
        Map<String, Object> options = readAppOptions(app.getOptions());
        String entryType = StringUtils.defaultIfBlank(text(options.get("entryType")), "OBJECT_LIST").toUpperCase();
        if (!APP_ENTRY_TYPES.contains(entryType)) {
            add(items, "APP_ENTRY_TYPE_INVALID", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "入口类型不合法", "入口类型仅支持对象列表、新增表单、详情、审批/待办、报表/看板、外链/API", entryType, null,
                    "CONFIG_APP_ENTRY", "调整入口类型", "publish", 323);
        }
        String permissionCode = text(options.get("permissionCode"));
        if (StringUtils.isBlank(permissionCode)) {
            add(items, "APP_ENTRY_PERMISSION_EMPTY", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "入口权限码未配置", "建议为每个入口配置独立权限码，方便多个入口指向同一业务对象时分开授权", null, null,
                    "CONFIG_APP_ENTRY", "配置入口权限", "publish", 323);
        }
        Set<String> pageKeys = BusinessPublishTargetCatalog.pageKeys(context.getPageSchema());
        Set<String> formKeys = BusinessPublishTargetCatalog.formKeys(readDesignerOptions(context));
        String targetPageKey = text(options.get("targetPageKey"));
        if (StringUtils.isNotBlank(targetPageKey) && !pageKeys.contains(targetPageKey)) {
            add(items, "APP_ENTRY_PAGE_MISSING", "APP_ENTRY", BusinessPublishCheckLevel.BLOCK,
                    "入口目标页面不存在", "访问入口引用了不存在的页面: " + targetPageKey, targetPageKey, null,
                    "CONFIG_APP_ENTRY", "修复入口页面", "publish", 323);
        }
        String targetFormKey = text(options.get("targetFormKey"));
        if (StringUtils.isNotBlank(targetFormKey) && !formKeys.contains(targetFormKey)) {
            add(items, "APP_ENTRY_FORM_MISSING", "APP_ENTRY", BusinessPublishCheckLevel.BLOCK,
                    "入口目标表单不存在", "访问入口引用了不存在的表单: " + targetFormKey, targetFormKey, null,
                    "CONFIG_APP_ENTRY", "修复入口表单", "publish", 324);
        }
        Map<String, Object> adminMenu = mapValue(options.get("adminMenu"));
        Object menuResourceId = firstNonNull(adminMenu.get("menuResourceId"), options.get("menuResourceId"));
        String mountTarget = StringUtils.defaultIfBlank(text(options.get("mountTarget")), "ADMIN");
        boolean syncEnabled = isTrue(firstNonNull(adminMenu.get("syncEnabled"), options.get("adminMenuSyncEnabled")));
        if ("ADMIN".equalsIgnoreCase(mountTarget) && syncEnabled
                && (menuResourceId == null || StringUtils.isBlank(String.valueOf(menuResourceId)))) {
            add(items, "APP_MENU_MISSING", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "菜单资源未同步", "未发现应用入口的菜单资源 ID，动态菜单可能无法保持选中态", null, null,
                    "CONFIG_APP_ENTRY", "同步菜单", "publish", 324);
        }
        Object runtimeOpenModeValue = firstNonNull(options.get("runtimeOpenMode"), adminMenu.get("runtimeOpenMode"));
        String runtimeOpenMode = text(runtimeOpenModeValue);
        if (StringUtils.isNotBlank(runtimeOpenMode)
                && !RUNTIME_OPEN_MODES.contains(runtimeOpenMode.trim().toUpperCase())) {
            add(items, "APP_RUNTIME_OPEN_MODE_EMPTY", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "运行打开模式不合法", "运行打开模式仅支持 LIST、CREATE_FORM、DETAIL，缺省时系统按 LIST 打开", null, null,
                    "CONFIG_APP_ENTRY", "配置打开方式", "publish", 325);
        }
        Map<String, Object> defaultParams = mapValue(options.get("defaultParams"));
        if (!defaultParams.isEmpty()) {
            for (String key : defaultParams.keySet()) {
                String lowerKey = StringUtils.lowerCase(key);
                if (StringUtils.containsAny(lowerKey, "password", "passwd", "token", "secret", "apikey", "accesskey")) {
                    add(items, "APP_ENTRY_PARAM_SENSITIVE", "APP_ENTRY", BusinessPublishCheckLevel.BLOCK,
                            "入口默认参数包含敏感键", "默认参数不能保存密码、Token 或密钥类长期凭证: " + key,
                            key, null, "CONFIG_APP_ENTRY", "清理默认参数", "publish", 326);
                }
            }
        }
        if (items.stream().noneMatch(item -> "APP_ENTRY".equals(item.getCategory())
                && !BusinessPublishCheckLevel.PASS.equals(item.getLevel()))) {
            add(items, "APP_ENTRY_PASS", "APP_ENTRY", BusinessPublishCheckLevel.PASS,
                    "应用入口检查通过", "运行入口、菜单资源和打开模式已具备基础配置", null, null,
                    null, null, "publish", 329);
        }
    }

    private void checkRuntimeDataSource(LowcodeModelSchema modelSchema, List<BusinessPublishCheckItemVO> items) {
        if (modelSchema == null) {
            add(items, "DATASOURCE_MODEL_EMPTY", "DATASOURCE", BusinessPublishCheckLevel.BLOCK,
                    "运行数据源无法解析", "模型协议为空，无法解析发布目标库", null, null,
                    "ADVANCED_CONFIG", "高级配置", "advanced", 390);
            return;
        }
        try {
            LowcodeRuntimeDataSourceContext context = runtimeDataSourceResolver.resolve(modelSchema);
            add(items, "DATASOURCE_TARGET_PASS", "DATASOURCE", BusinessPublishCheckLevel.PASS,
                    "发布目标库已确认",
                    "目标库: " + runtimeDatasourceLabel(context) + "；表: " + context.getTableName()
                            + "；主键: " + runtimePrimaryKeyLabel(context),
                    null, null, null, null, "publish", 391);
            if (context.isReadonly() || !context.isAllowWrite()) {
                add(items, "DATASOURCE_WRITE_DISABLED", "DATASOURCE", BusinessPublishCheckLevel.WARN,
                        "目标库未开放写入",
                        "运行态新增、编辑、删除和导入会被拦截；仅查询、导出和详情可用",
                        null, null, "CHECK_DATABASE", "检查数据源", "advanced", 392);
            }
            if (!context.isAllowDdl()) {
                add(items, "DATASOURCE_DDL_DISABLED", "DATASOURCE", BusinessPublishCheckLevel.WARN,
                        "目标库禁止在线 DDL",
                        "发布时不会在该数据源自动建表或变更字段，需由数据库管理员手工同步表结构",
                        null, null, "CHECK_DATABASE", "检查数据源", "advanced", 393);
            }
            if ("HIGH".equalsIgnoreCase(StringUtils.defaultString(context.getRiskLevel()))) {
                add(items, "DATASOURCE_HIGH_RISK", "DATASOURCE", BusinessPublishCheckLevel.WARN,
                        "高风险数据源",
                        "当前运行目标被标记为高风险，发布前请确认不是旧系统生产写库或已启用只读保护",
                        null, null, "CHECK_DATABASE", "检查数据源", "advanced", 394);
            }
        } catch (Exception e) {
            add(items, "DATASOURCE_UNAVAILABLE", "DATASOURCE", BusinessPublishCheckLevel.BLOCK,
                    "运行数据源不可用", e.getMessage(), null, null,
                    "CHECK_DATABASE", "检查数据源", "advanced", 390);
        }
    }

    private void checkTable(LowcodeModelSchema modelSchema, List<BusinessPublishCheckItemVO> items) {
        if (modelSchema == null || StringUtils.isBlank(modelSchema.getTableName())) {
            add(items, "TABLE_NAME_EMPTY", "TABLE", BusinessPublishCheckLevel.BLOCK,
                    "数据表缺失", "模型缺少运行数据表", null, null,
                    "ADVANCED_CONFIG", "高级配置", "advanced", 400);
            return;
        }
        try {
            LowcodeRuntimeDataSourceContext runtimeContext = runtimeDataSourceResolver.resolve(modelSchema);
            if (!ddlService.tableExists(modelSchema)) {
                boolean canOnlineDdl = hasPermission(DDL_PERMISSION);
                boolean canExecuteOnlineDdl = canOnlineDdl && runtimeContext.isAllowDdl();
                add(items, "TABLE_MISSING", "TABLE", canExecuteOnlineDdl ? BusinessPublishCheckLevel.WARN : BusinessPublishCheckLevel.BLOCK,
                        "数据表不存在", canExecuteOnlineDdl
                                ? "可在发布时勾选同步表结构自动创建到目标库: " + runtimeDatasourceLabel(runtimeContext)
                                : (!runtimeContext.isAllowDdl() ? "目标数据源禁止在线 DDL，请联系数据库管理员在 "
                                + runtimeDatasourceLabel(runtimeContext) + " 手工创建数据表"
                                : "缺少在线建表权限，请联系管理员同步表结构"),
                        null, null, "SYNC_TABLE", "同步表结构", "publish", 410);
                return;
            }
            if (!ddlService.hasSinglePrimaryKey(modelSchema)) {
                add(items, "TABLE_PK_MISSING", "TABLE", BusinessPublishCheckLevel.BLOCK,
                        "主键不符合要求", "业务表必须包含单字段主键", null, null,
                        "FIX_TABLE", "修复数据表", "advanced", 420);
                return;
            }
            List<String> retiredColumns = findRetiredBusinessColumns(modelSchema);
            if (!retiredColumns.isEmpty()) {
                add(items, "TABLE_COLUMN_RETIRED", "TABLE", BusinessPublishCheckLevel.WARN,
                        "存在已隐藏字段列", "字段已隐藏或停用，发布不会物理删除数据表列: " + String.join("、", retiredColumns),
                        null, null, "CHECK_FIELD", "检查字段", "fields", 425);
            }

            LowcodeDdlPreviewVO preview = ddlService.previewCreateTable(modelSchema);
            List<String> ddlStatements = preview.getDdlStatements();
            if (ddlStatements != null && !ddlStatements.isEmpty()) {
                boolean canOnlineDdl = hasPermission(DDL_PERMISSION);
                int manualReview = preview.getManualReviewDdlCount() == null
                        ? ddlService.countUnsafeDdl(modelSchema)
                        : preview.getManualReviewDdlCount();
                int onlineSafe = preview.getOnlineSafeDdlCount() == null
                        ? Math.max(0, ddlStatements.size() - manualReview)
                        : preview.getOnlineSafeDdlCount();
                boolean onlyHighRisk = onlineSafe <= 0 && manualReview > 0;
                boolean canExecuteOnlineDdl = canOnlineDdl && runtimeContext.isAllowDdl()
                        && Boolean.TRUE.equals(preview.getExecutable()) && onlineSafe > 0;
                String itemCode = onlyHighRisk
                        ? "TABLE_COLUMN_CHANGED" : resolveTableSyncItemCode(ddlStatements);
                String unavailableMessage = onlyHighRisk
                        ? "数据表差异均为高风险变更（改类型族/缩长度等），请在数据结构中导出脚本后人工审核: "
                        + summarizeDdlStatements(ddlStatements)
                        : (!runtimeContext.isAllowDdl() ? "目标数据源禁止在线 DDL，需手工同步: "
                        + summarizeDdlStatements(ddlStatements)
                        : "数据表结构与字段配置不一致且当前用户无在线同步权限: "
                        + summarizeDdlStatements(ddlStatements));
                String syncMessage = manualReview > 0
                        ? "发布时勾选同步数据表结构后，会在 " + runtimeDatasourceLabel(runtimeContext)
                        + " 自动执行 " + onlineSafe + " 条安全变更；另有 " + manualReview
                        + " 条高风险变更需导出人工审核: " + summarizeDdlStatements(ddlStatements)
                        : "发布时勾选同步数据表结构后，会在 " + runtimeDatasourceLabel(runtimeContext)
                        + " 自动执行受控变更: " + summarizeDdlStatements(ddlStatements);
                add(items, itemCode, "TABLE", canExecuteOnlineDdl ? BusinessPublishCheckLevel.WARN : BusinessPublishCheckLevel.BLOCK,
                        "数据表结构未同步",
                        canExecuteOnlineDdl ? syncMessage : unavailableMessage,
                        null, null, canExecuteOnlineDdl ? "SYNC_TABLE" : "REVIEW_DDL",
                        canExecuteOnlineDdl ? "同步表结构" : "查看数据库差异",
                        canExecuteOnlineDdl ? "publish" : "advanced", 430);
                if (!canExecuteOnlineDdl) {
                    return;
                }
            }
            add(items, "TABLE_PASS", "TABLE", BusinessPublishCheckLevel.PASS,
                    "数据表检查通过", "目标库 " + runtimeDatasourceLabel(runtimeContext)
                            + " 中数据表存在且主键符合低代码运行要求", null, null, null, null, "publish", 490);
        } catch (Exception e) {
            add(items, "TABLE_CHECK_WARN", "TABLE", BusinessPublishCheckLevel.WARN,
                    "数据表检查未完成", "当前环境无法完成数据表检查: " + e.getMessage(), null, null,
                    "CHECK_DATABASE", "检查数据库", "advanced", 430);
        }
    }

    private String runtimeDatasourceLabel(LowcodeRuntimeDataSourceContext context) {
        if (context == null) {
            return "未知数据源";
        }
        if (context.isMaster()) {
            return "平台主库";
        }
        return StringUtils.firstNonBlank(
                context.getDatasourceName(),
                context.getDatasourceCode(),
                context.getDatasourceId() == null ? null : String.valueOf(context.getDatasourceId()),
                "外部数据源"
        );
    }

    private String runtimePrimaryKeyLabel(LowcodeRuntimeDataSourceContext context) {
        if (context == null || context.getPrimaryKey() == null) {
            return "id";
        }
        String field = StringUtils.defaultIfBlank(context.getPrimaryKey().getField(), "id");
        String column = StringUtils.defaultIfBlank(context.getPrimaryKey().getColumnName(), field);
        return StringUtils.equals(field, column) ? column : field + "/" + column;
    }

    private List<String> findRetiredBusinessColumns(LowcodeModelSchema modelSchema) {
        if (modelSchema.getFields() == null) {
            return List.of();
        }
        Set<String> existingColumns = ddlService.listColumns(modelSchema);
        return modelSchema.getFields().stream()
                .filter(field -> field != null && !Boolean.TRUE.equals(field.getSystemField()))
                .filter(field -> "DISABLED".equalsIgnoreCase(StringUtils.defaultString(field.getFieldStatus()))
                        || "HIDDEN".equalsIgnoreCase(StringUtils.defaultString(field.getFieldStatus())))
                .map(LowcodeFieldSchema::getColumnName)
                .filter(StringUtils::isNotBlank)
                .filter(column -> !isSystemColumn(column))
                .filter(existingColumns::contains)
                .distinct()
                .toList();
    }

    private String resolveTableSyncItemCode(List<String> ddlStatements) {
        boolean hasModify = ddlStatements.stream().anyMatch(ddl -> StringUtils.containsIgnoreCase(ddl, " MODIFY COLUMN "));
        if (hasModify) {
            return "TABLE_COLUMN_CHANGED";
        }
        boolean hasAdd = ddlStatements.stream().anyMatch(ddl -> StringUtils.containsIgnoreCase(ddl, " ADD COLUMN "));
        if (hasAdd) {
            return "TABLE_COLUMN_MISSING";
        }
        return "TABLE_INDEX_MISSING";
    }

    private String summarizeDdlStatements(List<String> ddlStatements) {
        List<String> summary = ddlStatements.stream()
                .map(this::summarizeDdlStatement)
                .filter(StringUtils::isNotBlank)
                .limit(6)
                .toList();
        String suffix = ddlStatements.size() > summary.size() ? " 等 " + ddlStatements.size() + " 项" : "";
        return String.join("、", summary) + suffix;
    }

    private String summarizeDdlStatement(String ddl) {
        if (StringUtils.isBlank(ddl)) {
            return "";
        }
        String normalized = ddl.toUpperCase();
        if (normalized.contains(" ADD COLUMN ")) {
            return "新增列 " + extractBacktickValueAfter(ddl, "ADD COLUMN");
        }
        if (normalized.contains(" MODIFY COLUMN ")) {
            return "修改列 " + extractBacktickValueAfter(ddl, "MODIFY COLUMN");
        }
        if (normalized.contains(" ADD UNIQUE KEY ")) {
            return "新增唯一索引 " + extractBacktickValueAfter(ddl, "ADD UNIQUE KEY");
        }
        if (normalized.contains(" ADD KEY ")) {
            return "新增索引 " + extractBacktickValueAfter(ddl, "ADD KEY");
        }
        return ddl.length() > 80 ? ddl.substring(0, 80) + "..." : ddl;
    }

    private String extractBacktickValueAfter(String ddl, String marker) {
        String upper = ddl.toUpperCase();
        int markerIndex = upper.indexOf(marker);
        if (markerIndex < 0) {
            return "";
        }
        int start = ddl.indexOf('`', markerIndex + marker.length());
        int end = ddl.indexOf('`', start + 1);
        if (start < 0 || end <= start) {
            return "";
        }
        return "`" + ddl.substring(start + 1, end) + "`";
    }

    private boolean isSystemColumn(String columnName) {
        return "id".equals(columnName)
                || "tenant_id".equals(columnName)
                || "create_by".equals(columnName)
                || "create_time".equals(columnName)
                || "create_dept".equals(columnName)
                || "update_by".equals(columnName)
                || "update_time".equals(columnName)
                || "del_flag".equals(columnName);
    }

    private Map<String, Object> readDesignerOptions(BusinessObjectDesignerService.DesignerContext context) {
        if (context == null || context.getObject() == null
                || StringUtils.isBlank(context.getObject().getDesignerOptions())) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(context.getObject().getDesignerOptions(), new TypeReference<>() { });
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    private Map<String, Object> readAppOptions(String options) {
        if (StringUtils.isBlank(options)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(options, new TypeReference<>() { });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : new LinkedHashMap<>();
    }

    private boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(text(value)) || "1".equals(text(value));
    }

    private boolean hasPermission(String permission) {
        try {
            return SessionHelper.hasPermission(permission);
        } catch (Exception e) {
            return false;
        }
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long resolveTenantId(BusinessObjectDesignerService.DesignerContext context) {
        if (context != null && context.getObject() != null && context.getObject().getTenantId() != null) {
            return context.getObject().getTenantId();
        }
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }

    private void add(List<BusinessPublishCheckItemVO> items, String code, String category, String level,
                     String title, String message, String fieldCode, String zoneKey, String fixAction,
                     String fixActionLabel, String fixTarget, Integer sortOrder) {
        BusinessPublishCheckCollector.add(items, code, category, level, title, message, fieldCode, zoneKey,
                fixAction, fixActionLabel, fixTarget, sortOrder);
    }
}
