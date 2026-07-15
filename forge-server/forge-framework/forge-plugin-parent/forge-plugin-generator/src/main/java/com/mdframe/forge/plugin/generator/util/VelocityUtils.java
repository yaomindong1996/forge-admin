package com.mdframe.forge.plugin.generator.util;

import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Velocity模板引擎工具类
 */
public class VelocityUtils {

    /**
     * 初始化Velocity引擎
     */
    public static void initVelocity() {
        Properties props = new Properties();
        props.setProperty("resource.loader.file.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        props.setProperty("input.encoding", "UTF-8");
        props.setProperty("output.encoding", "UTF-8");
        Velocity.init(props);
    }

    /**
     * 准备Velocity上下文
     */
    public static VelocityContext prepareContext(GenTable genTable) {
        VelocityContext context = new VelocityContext();
        
        String moduleName = genTable.getModuleName();
        String businessName = genTable.getBusinessName();
        String packageName = genTable.getPackageName();
        String className = genTable.getClassName();
        String functionName = genTable.getFunctionName();
        String author = genTable.getAuthor();
        
        // 基础信息
        context.put("tableName", genTable.getTableName());
        context.put("tableComment", genTable.getTableComment());
        context.put("className", className);
        context.put("classname", GenUtils.getBusinessName(className)); // 首字母小写
        context.put("businessName", businessName);
        context.put("functionName", functionName);
        context.put("moduleName", moduleName);
        context.put("packageName", packageName);
        context.put("author", author);
        context.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        context.put("datetime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        context.put("menuParentId", resolveMenuParentId(genTable.getOptions()));
        
        // 列信息
        List<GenTableColumn> columns = genTable.getColumns();
        context.put("allColumns", columns);
        context.put("columns", getFilteredColumns(columns)); // 过滤掉基类字段
        context.put("pkColumn", GenUtils.getPkColumn(columns));

        // 前端模板信息
        String apiBase = "/" + moduleName + "/" + businessName;
        context.put("apiBase", apiBase);
        context.put("configKey", normalizeCssToken(moduleName + "-" + businessName));
        context.put("componentName", className);
        context.put("apiVarName", className);
        context.put("searchSchema", buildSearchSchema(columns));
        context.put("columnsSchema", buildColumnsSchema(columns));
        context.put("editSchema", buildEditSchema(columns));
        
        // 导入判断
        context.put("hasBigDecimal", GenUtils.hasBigDecimal(columns));
        context.put("hasDate", GenUtils.hasDate(columns));
        context.put("hasBaseEntity", GenUtils.hasBaseEntity(columns));
        context.put("hasTenantEntity", hasTenantEntity(columns));
        context.put("hasDictTrans", GenUtils.hasDictTrans(columns));
        context.put("hasDesensitize", false);
        context.put("hasEncrypt", false);
        context.put("enableDecrypt", false);
        context.put("enableEncrypt", false);
        
        // 模块路径
        context.put("modulePath", GenUtils.getModulePath(packageName, moduleName));
        
        return context;
    }

    private static Long resolveMenuParentId(Map<String, Object> options) {
        if (options == null) {
            return 0L;
        }
        Object value = options.get("menuParentId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && hasText(text)) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    /**
     * 过滤基类字段（如果继承BaseEntity）
     */
    private static List<GenTableColumn> getFilteredColumns(List<GenTableColumn> columns) {
        boolean hasBaseEntity = GenUtils.hasBaseEntity(columns);
        if (!hasBaseEntity) {
            return columns;
        }
        
        // 过滤掉BaseEntity中的字段
        List<String> baseFields = new ArrayList<>(Arrays.asList(
            "createTime", "createBy", "createDept", "updateTime", "updateBy"
        ));
        if (hasTenantEntity(columns)) {
            baseFields.add("tenantId");
        }
        
        return columns.stream()
            .filter(c -> !baseFields.contains(c.getJavaField()))
            .collect(java.util.stream.Collectors.toList());
    }

    private static boolean hasTenantEntity(List<GenTableColumn> columns) {
        return columns != null && columns.stream().anyMatch(column ->
                "tenantId".equals(column.getJavaField()) || "tenant_id".equals(column.getColumnName()));
    }

    /**
     * 获取模板列表
     */
    public static List<String> getTemplateList() {
        List<String> templates = new ArrayList<>();
        templates.add("templates/vm/entity.java.vm");
        templates.add("templates/vm/mapper.java.vm");
        templates.add("templates/vm/service.java.vm");
        templates.add("templates/vm/serviceImpl.java.vm");
        templates.add("templates/vm/controller.java.vm");
        templates.add("templates/vm/dto.java.vm");
        templates.add("templates/vm/query.java.vm");
        templates.add("templates/vm/mapper.xml.vm");
        templates.add("templates/vm/ai-crud/index.vue.vm");
        templates.add("templates/vm/ai-crud/api.js.vm");
        templates.add("templates/vm/sql/dict.sql.vm");
        templates.add("templates/vm/sql/menu.sql.vm");
        templates.add("templates/vm/sql/excel.sql.vm");
        return templates;
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String template, GenTable genTable) {
        String className = genTable.getClassName();
        String moduleName = genTable.getModuleName();
        String packageName = genTable.getPackageName();
        String businessName = genTable.getBusinessName();
        String javaPath = "main/java/" + packageName.replace(".", "/") + "/" + moduleName + "/";
        String resourcesPath = "main/resources/";
        
        if (template.contains("entity.java.vm")) {
            return javaPath + "entity/" + className + ".java";
        } else if (template.contains("mapper.java.vm")) {
            return javaPath + "mapper/" + className + "Mapper.java";
        } else if (template.contains("service.java.vm")) {
            return javaPath + "service/I" + className + "Service.java";
        } else if (template.contains("serviceImpl.java.vm")) {
            return javaPath + "service/impl/" + className + "ServiceImpl.java";
        } else if (template.contains("controller.java.vm")) {
            return javaPath + "controller/" + className + "Controller.java";
        } else if (template.contains("dto.java.vm")) {
            return javaPath + "dto/" + className + "DTO.java";
        } else if (template.contains("query.java.vm")) {
            return javaPath + "dto/" + className + "Query.java";
        } else if (template.contains("mapper.xml.vm")) {
            return resourcesPath + "mapper/" + className + "Mapper.xml";
        } else if (template.contains("ai-crud/index.vue.vm")) {
            return "frontend/src/views/" + moduleName + "/" + businessName + "/index.vue";
        } else if (template.contains("ai-crud/api.js.vm")) {
            return "frontend/src/api/" + moduleName + "/" + businessName + ".js";
        } else if (template.contains("sql/dict.sql.vm")) {
            return "sql/" + genTable.getTableName() + "_dict.sql";
        } else if (template.contains("sql/menu.sql.vm")) {
            return "sql/" + genTable.getTableName() + "_menu.sql";
        } else if (template.contains("sql/excel.sql.vm")) {
            return "sql/" + genTable.getTableName() + "_excel.sql";
        }
        
        return null;
    }

    /**
     * 渲染模板
     */
    public static String renderTemplate(String templatePath, VelocityContext context) {
        StringWriter writer = new StringWriter();
        Velocity.mergeTemplate(templatePath, "UTF-8", context, writer);
        return writer.toString();
    }

    private static List<Map<String, Object>> buildSearchSchema(List<GenTableColumn> columns) {
        List<Map<String, Object>> schema = new ArrayList<>();
        if (columns == null) {
            return schema;
        }
        for (GenTableColumn column : columns) {
            if (!isEnabled(column.getIsQuery())) {
                continue;
            }
            Map<String, Object> field = buildFormField(column);
            schema.add(field);
        }
        return schema;
    }

    private static List<Map<String, Object>> buildColumnsSchema(List<GenTableColumn> columns) {
        List<Map<String, Object>> schema = new ArrayList<>();
        if (columns == null) {
            return schema;
        }
        for (GenTableColumn column : columns) {
            if (!isEnabled(column.getIsList())) {
                continue;
            }
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("dataIndex", column.getJavaField());
            col.put("title", escapeJs(column.getColumnComment()));
            col.put("width", resolveColumnWidth(column));
            if (hasText(column.getDictType())) {
                col.put("_dictType", column.getDictType());
            }
            schema.add(col);
        }
        return schema;
    }

    private static List<Map<String, Object>> buildEditSchema(List<GenTableColumn> columns) {
        List<Map<String, Object>> schema = new ArrayList<>();
        if (columns == null) {
            return schema;
        }
        for (GenTableColumn column : columns) {
            if (!isEnabled(column.getIsInsert()) && !isEnabled(column.getIsEdit())) {
                continue;
            }
            schema.add(buildFormField(column));
        }
        return schema;
    }

    private static Map<String, Object> buildFormField(GenTableColumn column) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("field", column.getJavaField());
        field.put("label", escapeJs(column.getColumnComment()));
        field.put("type", resolveFormType(column));
        field.put("required", isEnabled(column.getIsRequired()));
        if (hasText(column.getDictType())) {
            field.put("dictType", column.getDictType());
        }
        return field;
    }

    private static String resolveFormType(GenTableColumn column) {
        if (hasText(column.getDictType())) {
            return "select";
        }

        String htmlType = column.getHtmlType();
        if (htmlType != null) {
            switch (htmlType.toUpperCase(Locale.ROOT)) {
                case "TEXTAREA":
                    return "textarea";
                case "SELECT":
                    return "select";
                case "RADIO":
                    return "radio";
                case "CHECKBOX":
                    return "checkbox";
                case "DATE":
                    return "date";
                case "DATETIME":
                    return "datetime";
                case "FILE_UPLOAD":
                case "FILEUPLOAD":
                    return "fileUpload";
                case "IMAGE_UPLOAD":
                case "IMAGEUPLOAD":
                    return "imageUpload";
                default:
                    break;
            }
        }

        String javaType = column.getJavaType();
        if ("Integer".equals(javaType) || "Long".equals(javaType)
                || "BigDecimal".equals(javaType) || "Float".equals(javaType) || "Double".equals(javaType)) {
            return "number";
        }
        if ("LocalDateTime".equals(javaType)) {
            return "datetime";
        }
        return "input";
    }

    private static Integer resolveColumnWidth(GenTableColumn column) {
        String javaType = column.getJavaType();
        if ("LocalDateTime".equals(javaType)) {
            return 180;
        }
        if (hasText(column.getColumnComment()) && column.getColumnComment().length() > 8) {
            return 180;
        }
        return 140;
    }

    private static boolean isEnabled(Integer value) {
        return value != null && value == 1;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String normalizeCssToken(String value) {
        if (!hasText(value)) {
            return "generated";
        }
        return value.replaceAll("[^a-zA-Z0-9_-]", "-");
    }

    private static String escapeJs(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
}
