package com.mdframe.forge.plugin.generator.util;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.plugin.generator.config.GeneratorConfig;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 代码生成器工具类
 */
public class GenUtils {

    /**
     * 数据库类型到Java类型映射
     */
    public static String convertDbTypeToJavaType(String columnType) {
        if (StringUtils.isEmpty(columnType)) {
            return "String";
        }
        
        String type = columnType.toLowerCase();
        
        if (type.contains("char") || type.contains("text")) {
            return "String";
        } else if (type.contains("bigint")) {
            return "Long";
        } else if (type.contains("int")) {
            return "Integer";
        } else if (type.contains("decimal") || type.contains("numeric")) {
            return "BigDecimal";
        } else if (type.contains("float")) {
            return "Float";
        } else if (type.contains("double")) {
            return "Double";
        } else if (type.contains("date") || type.contains("time") || type.contains("timestamp")) {
            return "LocalDateTime";
        } else if (type.contains("blob") || type.contains("binary")) {
            return "byte[]";
        } else if (type.contains("bit") || type.contains("boolean")) {
            return "Boolean";
        }
        
        return "String";
    }

    /**
     * 列名转换为Java字段名（驼峰命名）
     */
    public static String convertColumnNameToJavaField(String columnName) {
        return StrUtil.toCamelCase(columnName);
    }

    /**
     * 表名转换为Java类名
     */
    public static String convertTableNameToClassName(String tableName, GeneratorConfig config) {
        // 移除表前缀
        if (config.getAutoRemovePrefix() && StringUtils.isNotBlank(config.getTablePrefix())) {
            String[] prefixes = config.getTablePrefix().split(",");
            for (String prefix : prefixes) {
                if (tableName.startsWith(prefix)) {
                    tableName = tableName.substring(prefix.length());
                    break;
                }
            }
        }
        
        // 转换为帕斯卡命名（首字母大写的驼峰）
        return StrUtil.upperFirst(StrUtil.toCamelCase(tableName));
    }

    /**
     * 获取业务名称（首字母小写的类名）
     */
    public static String getBusinessName(String className) {
        return StrUtil.lowerFirst(className);
    }

    /**
     * 初始化表信息
     */
    public static void initTable(GenTable genTable, GeneratorConfig config) {
        genTable.setClassName(convertTableNameToClassName(genTable.getTableName(), config));
        genTable.setBusinessName(getBusinessName(genTable.getClassName()));
        genTable.setFunctionName(genTable.getTableComment());
        genTable.setPackageName(config.getPackageName());
        genTable.setModuleName(config.getModuleName());
        genTable.setAuthor(config.getAuthor());
        genTable.setTemplateEngine(config.getTemplateEngine());
        genTable.setGenType("DOWNLOAD");
        genTable.setGenPath("/");
    }

    /**
     * 初始化列信息
     */
    public static void initColumnField(GenTableColumn column) {
        String columnName = column.getColumnName();
        String columnType = column.getColumnType();
        String columnComment = column.getColumnComment();
        
        // 设置Java字段名和类型
        column.setJavaField(convertColumnNameToJavaField(columnName));
        column.setJavaType(convertDbTypeToJavaType(columnType));
        
        // isPk、isIncrement、isRequired 已经从数据库查询设置，不需要再次设置
        
        // 智能识别字典类型（从备注中解析）
        parseDictTypeFromComment(column, columnComment);
        
        // 设置查询类型
        if (column.getJavaType().equals("String")) {
            column.setQueryType("LIKE");
        } else {
            column.setQueryType("EQ");
        }
        
        // 设置HTML类型
        column.setHtmlType(getHtmlType(column.getJavaType()));
        
        // 排除基类字段
        boolean isSuper = isSuperColumn(columnName);
        column.setIsInsert(isSuper ? 0 : 1);
        column.setIsEdit(isSuper ? 0 : 1);
        column.setIsList(isSuper ? 0 : 1);
        column.setIsQuery(isSuper ? 0 : 1);
    }

    /**
     * 从字段备注中解析字典类型
     * 支持格式：状态(1:正常,0:停用) 或 状态(1-正常,0-停用)
     */
    private static void parseDictTypeFromComment(GenTableColumn column, String comment) {
        if (StringUtils.isBlank(comment)) {
            return;
        }
        
        // 检查是否包含字典定义格式 (value:label,value:label)
        int leftParen = comment.indexOf("(");
        int rightParen = comment.indexOf(")");
        
        if (leftParen >= 0 && rightParen > leftParen) {
            String dictItems = comment.substring(leftParen + 1, rightParen);
            // 判断是否是字典格式（包含:或-分隔符）
            if (dictItems.contains(":") || dictItems.contains("-")) {
                // 设置字典类型为字段名
                column.setDictType(column.getColumnName());
            }
        }
    }

    /**
     * 判断是否为基类字段
     */
    private static boolean isSuperColumn(String columnName) {
        List<String> superColumns = Arrays.asList(
            "create_time", "create_by", "update_time", "update_by", "remark"
        );
        return superColumns.contains(columnName.toLowerCase());
    }

    /**
     * 根据Java类型获取HTML类型
     */
    private static String getHtmlType(String javaType) {
        if ("Integer".equals(javaType) || "Long".equals(javaType)) {
            return "INPUT";
        } else if ("BigDecimal".equals(javaType) || "Float".equals(javaType) || "Double".equals(javaType)) {
            return "INPUT";
        } else if ("LocalDateTime".equals(javaType)) {
            return "DATETIME";
        } else if ("String".equals(javaType)) {
            return "INPUT";
        }
        return "INPUT";
    }

    /**
     * 获取模块路径
     */
    public static String getModulePath(String packageName, String moduleName) {
        return packageName.replace(".", "/") + "/" + moduleName;
    }

    /**
     * 获取包前缀
     */
    public static String getPackagePrefix(String packageName) {
        int lastIndex = packageName.lastIndexOf(".");
        return lastIndex != -1 ? packageName.substring(0, lastIndex) : packageName;
    }

    /**
     * 判断是否需要导入BigDecimal
     */
    public static boolean hasBigDecimal(List<GenTableColumn> columns) {
        return columns.stream().anyMatch(c -> "BigDecimal".equals(c.getJavaType()));
    }

    /**
     * 判断是否需要导入LocalDateTime
     */
    public static boolean hasDate(List<GenTableColumn> columns) {
        return columns.stream().anyMatch(c -> "LocalDateTime".equals(c.getJavaType()));
    }

    /**
     * 判断是否有基类
     */
    public static boolean hasBaseEntity(List<GenTableColumn> columns) {
        return columns.stream().anyMatch(c ->
            "createTime".equals(c.getJavaField()) ||
            "updateTime".equals(c.getJavaField())
        );
    }

    /**
     * 判断是否需要字典翻译
     */
    public static boolean hasDictTrans(List<GenTableColumn> columns) {
        return columns.stream().anyMatch(c -> StringUtils.isNotBlank(c.getDictType()));
    }

    /**
     * 获取主键列
     */
    public static GenTableColumn getPkColumn(List<GenTableColumn> columns) {
        return columns.stream()
            .filter(c -> c.getIsPk() != null && c.getIsPk() == 1)
            .findFirst()
            .orElse(null);
    }
}
