package com.mdframe.forge.plugin.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.data.dto.DataConnectionSaveDTO;
import com.mdframe.forge.plugin.data.dto.DataConnectionTestDTO;
import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.plugin.data.service.DataConnectionService;
import com.mdframe.forge.plugin.data.support.DbDialectFactory;
import com.mdframe.forge.plugin.data.support.JdbcDataSourceProvider;
import com.mdframe.forge.plugin.data.vo.DataConnectionDetailVO;
import com.mdframe.forge.plugin.data.vo.DataConnectionFieldVO;
import com.mdframe.forge.plugin.data.vo.DataConnectionTableVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/data/connection")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class DataConnectionController {

    private final DataConnectionService connectionService;
    private final JdbcDataSourceProvider dataSourceProvider;
    private final DbDialectFactory dialectFactory;

    @GetMapping("/page")
    public RespInfo<IPage<DataConnection>> page(
            @RequestParam(required = false) String connectionName,
            @RequestParam(required = false) String dbType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<DataConnection> page = connectionService.page(connectionName, dbType, status, pageNum, pageSize);
        page.getRecords().forEach(this::maskSensitive);
        return RespInfo.success(page);
    }

    @GetMapping("/list")
    public RespInfo<List<DataConnection>> list() {
        List<DataConnection> list = connectionService.listAll();
        list.forEach(this::maskSensitive);
        return RespInfo.success(list);
    }

    @GetMapping("/{id}")
    public RespInfo<DataConnectionDetailVO> getById(@PathVariable Long id) {
        DataConnection connection = connectionService.getById(id);
        if (connection == null) {
            throw new BusinessException("数据连接不存在或已删除");
        }
        return RespInfo.success(convertToDetailVO(connection));
    }

    @PostMapping
    @OperationLog(module = "数据资产", desc = "新增数据连接：{{#dto.connectionName}}")
    public RespInfo<Void> add(@Validated @RequestBody DataConnectionSaveDTO dto) {
        validateConnection(dto);
        connectionService.saveConnection(dto);
        return RespInfo.success();
    }

    @PutMapping
    @OperationLog(module = "数据资产", desc = "修改数据连接：{{#dto.connectionName}}")
    public RespInfo<Void> edit(@Validated @RequestBody DataConnectionSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("数据连接ID不能为空");
        }
        validateConnection(dto);
        connectionService.updateConnection(dto);
        dataSourceProvider.closeDataSource(dto.getId());
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "数据资产", desc = "删除数据连接")
    public RespInfo<Void> remove(@PathVariable Long id) {
        if (connectionService.hasDatasetReference(id)) {
            throw new BusinessException("该数据连接已被数据集引用，无法删除");
        }
        connectionService.removeById(id);
        dataSourceProvider.closeDataSource(id);
        return RespInfo.success();
    }

    @PostMapping("/{id}/test")
    @OperationLog(module = "数据资产", desc = "测试数据连接")
    public RespInfo<Boolean> testSaved(@PathVariable Long id) {
        DataConnection connection = connectionService.getById(id);
        if (connection == null) {
            throw new BusinessException("数据连接不存在或已删除");
        }
        if (connection.getStatus() != 1) {
            throw new BusinessException("数据连接已禁用");
        }
        boolean success = doTestConnection(connection);
        return RespInfo.success(success);
    }

    @PostMapping("/test")
    public RespInfo<Boolean> testTemp(@RequestBody DataConnectionTestDTO dto) {
        DataConnection connection = new DataConnection();
        connection.setDriverClassName(dto.getDriverClassName());
        connection.setJdbcUrl(dto.getJdbcUrl());
        connection.setUsername(dto.getUsername());
        connection.setPasswordCipher(dto.getPassword());
        connection.setTestSql(dto.getTestSql() != null ? dto.getTestSql() : "SELECT 1");
        boolean success = doTestConnectionTemp(connection);
        return RespInfo.success(success);
    }

    @GetMapping("/{id}/tables")
    public RespInfo<List<DataConnectionTableVO>> getTables(
            @PathVariable Long id,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        DataConnection connection = connectionService.getById(id);
        if (connection == null) {
            throw new BusinessException("数据连接不存在或已删除");
        }
        if (connection.getStatus() != 1) {
            throw new BusinessException("数据连接已禁用");
        }
        List<DataConnectionTableVO> tables = queryTables(connection, keyword);
        return RespInfo.success(tables);
    }

    @GetMapping("/{id}/tables/{tableName}/fields")
    public RespInfo<List<DataConnectionFieldVO>> getFields(
            @PathVariable Long id,
            @PathVariable String tableName) {
        DataConnection connection = connectionService.getById(id);
        if (connection == null) {
            throw new BusinessException("数据连接不存在或已删除");
        }
        if (connection.getStatus() != 1) {
            throw new BusinessException("数据连接已禁用");
        }
        List<DataConnectionFieldVO> fields = queryFields(connection, tableName);
        return RespInfo.success(fields);
    }

    private void validateConnection(DataConnectionSaveDTO dto) {
        if (dto.getConnectionCode() == null || dto.getConnectionCode().isEmpty()) {
            throw new BusinessException("连接编码不能为空");
        }
        if (dto.getConnectionName() == null || dto.getConnectionName().isEmpty()) {
            throw new BusinessException("连接名称不能为空");
        }
        if (dto.getDbType() == null || dto.getDbType().isEmpty()) {
            throw new BusinessException("数据库类型不能为空");
        }
        if (dto.getJdbcUrl() == null || dto.getJdbcUrl().isEmpty()) {
            throw new BusinessException("JDBC连接地址不能为空");
        }
        if (dto.getUsername() == null || dto.getUsername().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (dto.getId() == null && (dto.getPassword() == null || dto.getPassword().isEmpty())) {
            throw new BusinessException("密码不能为空");
        }
    }

    private DataConnectionDetailVO convertToDetailVO(DataConnection connection) {
        DataConnectionDetailVO vo = new DataConnectionDetailVO();
        vo.setId(connection.getId());
        vo.setConnectionCode(connection.getConnectionCode());
        vo.setConnectionName(connection.getConnectionName());
        vo.setDbType(connection.getDbType());
        vo.setDriverClassName(connection.getDriverClassName());
        vo.setJdbcUrl(maskJdbcUrl(connection.getJdbcUrl()));
        vo.setUsername(connection.getUsername());
        vo.setHasPassword(connection.getPasswordCipher() != null && !connection.getPasswordCipher().isEmpty());
        vo.setSchemaName(connection.getSchemaName());
        vo.setTestSql(connection.getTestSql());
        vo.setPoolConfigJson(connection.getPoolConfigJson());
        vo.setStatus(connection.getStatus());
        vo.setDescription(connection.getDescription());
        vo.setCreateTime(connection.getCreateTime());
        vo.setUpdateTime(connection.getUpdateTime());
        return vo;
    }

    private void maskSensitive(DataConnection connection) {
        connection.setPasswordCipher(null);
        connection.setJdbcUrl(maskJdbcUrl(connection.getJdbcUrl()));
    }

    private String maskJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        return jdbcUrl.replaceAll("password=[^&]*", "password=***")
                .replaceAll("pwd=[^&]*", "pwd=***");
    }

    private boolean doTestConnection(DataConnection connection) {
        try {
            log.info("Testing saved connection id={}, dbType={}, url={}, username={}", 
                    connection.getId(), connection.getDbType(), 
                    maskJdbcUrl(connection.getJdbcUrl()), connection.getUsername());
            Connection conn = dataSourceProvider.getConnection(connection);
            try {
                PreparedStatement ps = conn.prepareStatement(connection.getTestSql());
                try {
                    ResultSet rs = ps.executeQuery();
                    rs.close();
                    ps.close();
                    log.info("Connection test success for id={}", connection.getId());
                    return true;
                } finally {
                    ps.close();
                }
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            log.error("Test saved connection failed for id={}: {}", 
                    connection.getId(), e.getMessage(), e);
            return false;
        }
    }

    private boolean doTestConnectionTemp(DataConnection connection) {
        DataSource ds = null;
        try {
            ds = dataSourceProvider.createTempDataSource(connection, connection.getPasswordCipher());
            Connection conn = ds.getConnection();
            try {
                PreparedStatement ps = conn.prepareStatement(connection.getTestSql());
                try {
                    ResultSet rs = ps.executeQuery();
                    rs.close();
                    ps.close();
                    return true;
                } finally {
                    ps.close();
                }
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            log.warn("Test temp connection failed: {}", e.getMessage());
            return false;
        } finally {
            if (ds instanceof com.zaxxer.hikari.HikariDataSource) {
                ((com.zaxxer.hikari.HikariDataSource) ds).close();
            }
        }
    }

    private List<DataConnectionTableVO> queryTables(DataConnection connection, String keyword) {
        List<DataConnectionTableVO> tables = new ArrayList<>();
        String schemaName = connection.getSchemaName();
        if (schemaName == null || schemaName.isEmpty()) {
            schemaName = extractSchemaFromUrl(connection.getJdbcUrl());
        }
        try {
            Connection conn = dataSourceProvider.getConnection(connection);
            try {
                String sql = dialectFactory.getDialect(connection.getDbType()).getTableQuerySql(schemaName, keyword);
                PreparedStatement ps = conn.prepareStatement(sql);
                try {
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        DataConnectionTableVO table = new DataConnectionTableVO();
                        table.setTableName(rs.getString("tableName"));
                        table.setTableType(rs.getString("tableType"));
                        table.setTableComment(rs.getString("tableComment"));
                        tables.add(table);
                    }
                    rs.close();
                } finally {
                    ps.close();
                }
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            log.warn("Query tables failed for connection id={}: {}", connection.getId(), e.getMessage());
        }
        return tables;
    }

    private List<DataConnectionFieldVO> queryFields(DataConnection connection, String tableName) {
        List<DataConnectionFieldVO> fields = new ArrayList<>();
        String schemaName = connection.getSchemaName();
        if (schemaName == null || schemaName.isEmpty()) {
            schemaName = extractSchemaFromUrl(connection.getJdbcUrl());
        }
        try {
            Connection conn = dataSourceProvider.getConnection(connection);
            try {
                String sql = dialectFactory.getDialect(connection.getDbType()).getColumnQuerySql(schemaName, tableName);
                PreparedStatement ps = conn.prepareStatement(sql);
                try {
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        DataConnectionFieldVO field = new DataConnectionFieldVO();
                        field.setColumnName(rs.getString("columnName"));
                        field.setColumnType(rs.getString("columnType"));
                        field.setColumnComment(rs.getString("columnComment"));
                        field.setNullable("YES".equalsIgnoreCase(rs.getString("nullable")));
                        field.setPrimaryKey("PRI".equalsIgnoreCase(rs.getString("primaryKey")));
                        fields.add(field);
                    }
                    rs.close();
                } finally {
                    ps.close();
                }
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            log.warn("Query fields failed for connection id={}, table={}: {}", connection.getId(), tableName, e.getMessage());
        }
        return fields;
    }

    private String extractSchemaFromUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        int start = jdbcUrl.lastIndexOf('/');
        if (start < 0) {
            return null;
        }
        int end = jdbcUrl.indexOf('?');
        if (end < 0) {
            end = jdbcUrl.length();
        }
        return jdbcUrl.substring(start + 1, end);
    }
}
