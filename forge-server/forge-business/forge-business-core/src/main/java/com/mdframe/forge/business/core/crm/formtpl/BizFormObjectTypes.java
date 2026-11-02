package com.mdframe.forge.business.core.crm.formtpl;

import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 业务表单 objectType 注册表：scheme 读权限、配置写权限、扩展值存储策略。
 */
public final class BizFormObjectTypes {

    public enum ExtStorage {
        /** CRM 主表 ext_json 列 */
        COLUMN_EXT_JSON,
        /** 系统侧表 sys_biz_form_ext_data */
        SIDE_TABLE
    }

    public record Def(
            String objectType,
            String schemePermission,
            String editPermission,
            ExtStorage storage
    ) {
    }

    private static final String CRM_EDIT = "crm:biz-form:edit";
    private static final String SYS_EDIT = "system:biz-form:edit";

    private static final Map<String, Def> REGISTRY;

    static {
        Map<String, Def> map = new LinkedHashMap<>();
        // CRM
        put(map, "PROJECT", "crm:project:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "CUSTOMER", "crm:customer:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "CONTACT", "crm:contact:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "OPPORTUNITY", "crm:opportunity:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "CONTRACT", "crm:contract:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "SUPPLIER", "crm:supplier:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "PRODUCT", "crm:product:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "ACTIVITY", "crm:activity:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "TRADE", "crm:trade:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "FINANCE", "crm:finance:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "FINANCE_ACCOUNT", "crm:finance:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "SUBJECT", "crm:subject:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "STAMP", "crm:stamp:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "PURCHASE_ORDER", "crm:purchase-order:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "PROCUREMENT_PLAN", "crm:procurement:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "FUNDING_PLAN", "crm:funding-plan:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "LOAN", "crm:loan:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "EXPENSE", "crm:expense:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "BUDGET", "crm:budget:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "LEAVE", "crm:leave:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "ATTENDANCE", "crm:attendance:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "GOAL", "crm:goal:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "SCHEDULE", "crm:schedule:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "SERVICE_KNOWLEDGE", "crm:service-knowledge:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        put(map, "DOCUMENT_TEMPLATE", "crm:document-template:list", CRM_EDIT, ExtStorage.COLUMN_EXT_JSON);
        // SYSTEM
        put(map, "USER", "system:user:list", SYS_EDIT, ExtStorage.SIDE_TABLE);
        put(map, "ROLE", "system:role:list", SYS_EDIT, ExtStorage.SIDE_TABLE);
        put(map, "ORG", "system:org:list", SYS_EDIT, ExtStorage.SIDE_TABLE);
        put(map, "POST", "system:post:list", SYS_EDIT, ExtStorage.SIDE_TABLE);
        put(map, "DICT_TYPE", "system:dict:list", SYS_EDIT, ExtStorage.SIDE_TABLE);
        put(map, "DICT_DATA", "system:dict:list", SYS_EDIT, ExtStorage.SIDE_TABLE);
        put(map, "NOTICE", "system:notice:list", SYS_EDIT, ExtStorage.SIDE_TABLE);
        put(map, "CLIENT", "system:client:list", SYS_EDIT, ExtStorage.SIDE_TABLE);
        put(map, "TENANT", "system:tenant:list", SYS_EDIT, ExtStorage.SIDE_TABLE);
        put(map, "CONFIG", "system:config:list", SYS_EDIT, ExtStorage.SIDE_TABLE);
        put(map, "STORAGE_CONFIG", "system:storage:list", SYS_EDIT, ExtStorage.SIDE_TABLE);
        put(map, "API_CONFIG", "system:api-config:list", SYS_EDIT, ExtStorage.SIDE_TABLE);
        put(map, "MENU", "system:menu:list", SYS_EDIT, ExtStorage.SIDE_TABLE);
        put(map, "JOB_CONFIG", "system:job:list", SYS_EDIT, ExtStorage.SIDE_TABLE);
        REGISTRY = Collections.unmodifiableMap(map);
    }

    private BizFormObjectTypes() {
    }

    private static void put(Map<String, Def> map, String type, String schemePerm, String editPerm, ExtStorage storage) {
        map.put(type, new Def(type, schemePerm, editPerm, storage));
    }

    public static String normalize(String objectType) {
        if (!StringUtils.hasText(objectType)) {
            throw new BusinessException("业务对象不能为空");
        }
        return objectType.trim().toUpperCase(Locale.ROOT);
    }

    public static Def require(String objectType) {
        String type = normalize(objectType);
        Def def = REGISTRY.get(type);
        if (def == null) {
            throw new BusinessException("未注册的表单对象类型：" + type);
        }
        return def;
    }

    public static boolean isRegistered(String objectType) {
        if (!StringUtils.hasText(objectType)) {
            return false;
        }
        return REGISTRY.containsKey(objectType.trim().toUpperCase(Locale.ROOT));
    }

    public static Set<String> allTypes() {
        return REGISTRY.keySet();
    }

    public static void checkSchemePermission(String objectType) {
        Def def = require(objectType);
        if (StpUtil.hasPermission(def.schemePermission()) || StpUtil.hasPermission(def.editPermission())) {
            return;
        }
        StpUtil.checkPermission(def.schemePermission());
    }

    public static void checkEditPermission(String objectType) {
        Def def = require(objectType);
        StpUtil.checkPermission(def.editPermission());
    }
}
