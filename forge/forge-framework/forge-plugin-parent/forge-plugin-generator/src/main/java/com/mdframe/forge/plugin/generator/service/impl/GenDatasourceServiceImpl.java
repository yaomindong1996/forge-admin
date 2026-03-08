package com.mdframe.forge.plugin.generator.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.mapper.GenDatasourceMapper;
import com.mdframe.forge.plugin.generator.service.IGenDatasourceService;
import com.mdframe.forge.plugin.generator.util.DynamicDataSourceUtil;
import com.mdframe.forge.plugin.generator.util.GenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 代码生成器数据源配置Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenDatasourceServiceImpl extends ServiceImpl<GenDatasourceMapper, GenDatasource> implements IGenDatasourceService {

    /**
     * AES加密密钥（16字节）
     */
    private static final String AES_KEY = "ForgeGenerator16";

    private final GenDatasourceMapper genDatasourceMapper;

    @Override
    public boolean save(GenDatasource entity) {
        // 保存前加密密码
        if (StrUtil.isNotBlank(entity.getPassword())) {
            entity.setPassword(encryptPassword(entity.getPassword()));
        }
        return super.save(entity);
    }

    @Override
    public boolean updateById(GenDatasource entity) {
        // 更新时如果密码不为空，则加密
        if (StrUtil.isNotBlank(entity.getPassword())) {
            entity.setPassword(encryptPassword(entity.getPassword()));
        }
        return super.updateById(entity);
    }

    @Override
    public boolean testConnection(Long datasourceId) {
        GenDatasource datasource = genDatasourceMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new RuntimeException("数据源不存在");
        }
        // 解密密码用于测试连接
        if (StrUtil.isNotBlank(datasource.getPassword())) {
            datasource.setPassword(decryptPassword(datasource.getPassword()));
        }
        return DynamicDataSourceUtil.testConnection(datasource);
    }

    @Override
    public List<GenTable> selectDbTableList(Long datasourceId) {
        GenDatasource datasource = getDatasourceById(datasourceId);
        List<GenTable> tables = new ArrayList<>();

        String sql = "SELECT table_name, table_comment, create_time, update_time " +
                     "FROM information_schema.tables " +
                     "WHERE table_schema = DATABASE() " +
                     "AND table_type = 'BASE TABLE' " +
                     "AND table_name NOT LIKE 'qrtz_%' AND table_name NOT LIKE 'gen_%' " +
                     "ORDER BY create_time DESC";

        try (Connection conn = DynamicDataSourceUtil.getConnection(datasource);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                GenTable table = new GenTable();
                table.setTableName(rs.getString("table_name"));
                table.setTableComment(rs.getString("table_comment"));
                table.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                tables.add(table);
            }
        } catch (Exception e) {
            log.error("查询数据源表列表失败: datasourceId={}", datasourceId, e);
            throw new RuntimeException("查询表列表失败: " + e.getMessage());
        }

        return tables;
    }

    @Override
    public GenTable selectDbTableByName(Long datasourceId, String tableName) {
        GenDatasource datasource = getDatasourceById(datasourceId);

        String sql = "SELECT table_name, table_comment, create_time, update_time " +
                     "FROM information_schema.tables " +
                     "WHERE table_schema = DATABASE() " +
                     "AND table_name = '" + tableName + "'";

        try (Connection conn = DynamicDataSourceUtil.getConnection(datasource);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                GenTable table = new GenTable();
                table.setTableName(rs.getString("table_name"));
                table.setTableComment(rs.getString("table_comment"));
                table.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                return table;
            }
        } catch (Exception e) {
            log.error("查询表信息失败: datasourceId={}, tableName={}", datasourceId, tableName, e);
            throw new RuntimeException("查询表信息失败: " + e.getMessage());
        }

        return null;
    }

    @Override
    public List<GenTableColumn> selectDbTableColumnsByName(Long datasourceId, String tableName) {
        GenDatasource datasource = getDatasourceById(datasourceId);
        List<GenTableColumn> columns = new ArrayList<>();

        String sql = "SELECT " +
                     "    column_name, " +
                     "    column_comment, " +
                     "    column_type, " +
                     "    (CASE WHEN column_key = 'PRI' THEN 1 ELSE 0 END) AS is_pk, " +
                     "    (CASE WHEN extra = 'auto_increment' THEN 1 ELSE 0 END) AS is_increment, " +
                     "    (CASE WHEN is_nullable = 'NO' AND column_key != 'PRI' THEN 1 ELSE 0 END) AS is_required " +
                     "FROM information_schema.columns " +
                     "WHERE table_schema = DATABASE() " +
                     "AND table_name = '" + tableName + "' " +
                     "ORDER BY ordinal_position";

        try (Connection conn = DynamicDataSourceUtil.getConnection(datasource);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                GenTableColumn column = new GenTableColumn();
                column.setColumnName(rs.getString("column_name"));
                column.setColumnComment(rs.getString("column_comment"));
                column.setColumnType(rs.getString("column_type"));
                column.setIsPk(rs.getInt("is_pk"));
                column.setIsIncrement(rs.getInt("is_increment"));
                column.setIsRequired(rs.getInt("is_required"));

                // 初始化字段配置
                GenUtils.initColumnField(column);

                columns.add(column);
            }
        } catch (Exception e) {
            log.error("查询表字段信息失败: datasourceId={}, tableName={}", datasourceId, tableName, e);
            throw new RuntimeException("查询表字段信息失败: " + e.getMessage());
        }

        return columns;
    }

    @Override
    public GenDatasource getDefaultDatasource() {
        return genDatasourceMapper.selectOne(
            new LambdaQueryWrapper<GenDatasource>()
                .eq(GenDatasource::getIsDefault, 1)
                .eq(GenDatasource::getIsEnabled, 1)
                .last("LIMIT 1")
        );
    }

    /**
     * 根据ID获取数据源
     */
    private GenDatasource getDatasourceById(Long datasourceId) {
        GenDatasource datasource = genDatasourceMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new RuntimeException("数据源不存在: datasourceId=" + datasourceId);
        }
        if (datasource.getIsEnabled() == 0) {
            throw new RuntimeException("数据源已禁用: " + datasource.getDatasourceName());
        }
        // 解密密码用于连接
        if (StrUtil.isNotBlank(datasource.getPassword())) {
            datasource.setPassword(decryptPassword(datasource.getPassword()));
        }
        return datasource;
    }

    /**
     * 加密密码
     *
     * @param password 明文密码
     * @return 加密后的密码（十六进制）
     */
    private String encryptPassword(String password) {
        if (StrUtil.isBlank(password)) {
            return password;
        }
        try {
            AES aes = SecureUtil.aes(AES_KEY.getBytes(StandardCharsets.UTF_8));
            return aes.encryptHex(password);
        } catch (Exception e) {
            log.error("密码加密失败", e);
            throw new RuntimeException("密码加密失败");
        }
    }

    /**
     * 解密密码
     *
     * @param encryptedPassword 加密的密码
     * @return 解密后的明文密码
     */
    private String decryptPassword(String encryptedPassword) {
        if (StrUtil.isBlank(encryptedPassword)) {
            return encryptedPassword;
        }
        try {
            AES aes = SecureUtil.aes(AES_KEY.getBytes(StandardCharsets.UTF_8));
            return aes.decryptStr(encryptedPassword);
        } catch (Exception e) {
            log.warn("密码解密失败，可能是未加密的旧数据: {}", e.getMessage());
            // 如果解密失败，返回原密码（兼容未加密的旧数据）
            return encryptedPassword;
        }
    }
}
