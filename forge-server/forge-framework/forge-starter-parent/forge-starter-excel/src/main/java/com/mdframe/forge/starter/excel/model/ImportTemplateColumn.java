package com.mdframe.forge.starter.excel.model;

/**
 * 导入模板列说明。
 *
 * @param fieldName   业务字段编码
 * @param columnName  Excel 表头名称
 * @param required    是否必填
 * @param exampleValue 样例值
 * @param description 填写说明
 */
public record ImportTemplateColumn(
        String fieldName,
        String columnName,
        boolean required,
        String exampleValue,
        String description
) {
}
