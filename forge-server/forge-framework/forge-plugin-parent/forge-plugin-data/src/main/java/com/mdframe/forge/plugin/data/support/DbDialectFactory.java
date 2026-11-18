package com.mdframe.forge.plugin.data.support;

import com.mdframe.forge.plugin.data.enums.DbTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class DbDialectFactory {

    private final Map<String, DbDialect> dialectMap = new HashMap<>();

    @Autowired
    public DbDialectFactory(MySqlDialect mysqlDialect,
                            PostgreSqlDialect postgreSqlDialect,
                            SqlServerDialect sqlServerDialect) {
        dialectMap.put(DbTypeEnum.MYSQL.getCode(), mysqlDialect);
        dialectMap.put(DbTypeEnum.POSTGRESQL.getCode(), postgreSqlDialect);
        dialectMap.put(DbTypeEnum.SQLSERVER.getCode(), sqlServerDialect);
        // 兼容前端/连接串常见别名
        dialectMap.put("POSTGRES", postgreSqlDialect);
        dialectMap.put("PG", postgreSqlDialect);
        dialectMap.put("MSSQL", sqlServerDialect);
        dialectMap.put("SQL_SERVER", sqlServerDialect);
    }

    public DbDialect getDialect(String dbType) {
        String normalized = normalize(dbType);
        DbDialect dialect = dialectMap.get(normalized);
        if (dialect == null) {
            throw new IllegalArgumentException("不支持的数据库类型: " + dbType
                    + "（当前已注册: MYSQL, POSTGRESQL, SQLSERVER）");
        }
        return dialect;
    }

    private static String normalize(String dbType) {
        if (dbType == null || dbType.isBlank()) {
            return DbTypeEnum.MYSQL.getCode();
        }
        String value = dbType.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("POSTGRES".equals(value) || "PG".equals(value) || "POSTGRESQL".equals(value)) {
            return DbTypeEnum.POSTGRESQL.getCode();
        }
        if ("MYSQL".equals(value) || "MARIADB".equals(value)) {
            return DbTypeEnum.MYSQL.getCode();
        }
        if ("SQLSERVER".equals(value) || "MSSQL".equals(value) || "SQL_SERVER".equals(value)
                || "MICROSOFT_SQL_SERVER".equals(value)) {
            return DbTypeEnum.SQLSERVER.getCode();
        }
        return value;
    }
}
