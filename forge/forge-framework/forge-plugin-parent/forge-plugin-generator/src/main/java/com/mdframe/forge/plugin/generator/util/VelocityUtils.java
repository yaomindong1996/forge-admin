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
        context.put("datetime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 列信息
        List<GenTableColumn> columns = genTable.getColumns();
        context.put("columns", getFilteredColumns(columns)); // 过滤掉基类字段
        context.put("pkColumn", GenUtils.getPkColumn(columns));
        
        // 导入判断
        context.put("hasBigDecimal", GenUtils.hasBigDecimal(columns));
        context.put("hasDate", GenUtils.hasDate(columns));
        context.put("hasBaseEntity", GenUtils.hasBaseEntity(columns));
        context.put("hasDictTrans", GenUtils.hasDictTrans(columns));
        
        // 模块路径
        context.put("modulePath", GenUtils.getModulePath(packageName, moduleName));
        
        return context;
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
        List<String> baseFields = Arrays.asList(
            "createTime", "createBy", "updateTime", "updateBy", "remark"
        );
        
        return columns.stream()
            .filter(c -> !baseFields.contains(c.getJavaField()))
            .collect(java.util.stream.Collectors.toList());
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
}
