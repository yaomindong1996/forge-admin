package com.mdframe.forge.starter.tenant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 多租户配置属性
 */
@Data
@ConfigurationProperties(prefix = "forge.tenant")
public class TenantProperties {

    /**
     * 是否启用多租户
     */
    private Boolean enabled = true;

    /**
     * 租户字段名
     */
    private String column = "tenant_id";

    /**
     * 忽略租户的表名列表
     * 这些表不会自动添加租户条件
     */
    private List<String> ignoreTables = new ArrayList<>();

    /**
     * 忽略租户的SQL关键字
     * 包含这些关键字的SQL不会添加租户条件
     */
    private List<String> ignoreSqlKeywords = new ArrayList<>();

    /**
     * 是否在没有租户ID时抛出异常
     * true: 抛出异常（严格模式）
     * false: 记录警告日志（宽松模式）
     */
    private Boolean strictMode = false;

    public TenantProperties() {
        // 默认忽略系统配置表
        ignoreTables.add("sys_tenant");
        ignoreTables.add("sys_excel_column_config");
        ignoreTables.add("sys_excel_export_config");
        ignoreTables.add("sys_file_metadata");
        ignoreTables.add("sys_file_storage_config");
        ignoreTables.add("sys_job_log");
        ignoreTables.add("sys_job_config");
        ignoreTables.add("gen_datasource");
        ignoreTables.add("gen_table");
        ignoreTables.add("gen_table_column");
        ignoreTables.add("worker_node");
        ignoreTables.add("sys_id_sequence");
        ignoreTables.add("sys_notice_org");
        ignoreTables.add("sys_notice_org");
        ignoreTables.add("sys_notice_read_record");
        ignoreTables.add("sys_api_config");
        ignoreTables.add("sys_config_group");
        ignoreTables.add("sys_file_group");
    }
}
