package com.mdframe.forge.plugin.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report_data_connection")
public class DataConnection extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String connectionCode;

    private String connectionName;

    private String dbType;

    private String driverClassName;

    private String jdbcUrl;

    private String username;

    private String passwordCipher;

    private String schemaName;

    private String testSql;

    private String poolConfigJson;

    private Integer status;

    private String description;

    private Long createDept;
}