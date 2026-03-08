package com.mdframe.forge.plugin.generator.service.impl;

import cn.hutool.core.io.IoUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.config.GeneratorConfig;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper;
import com.mdframe.forge.plugin.generator.mapper.GenTableMapper;
import com.mdframe.forge.plugin.generator.service.IGenDatasourceService;
import com.mdframe.forge.plugin.generator.service.IGenTableService;
import com.mdframe.forge.plugin.generator.util.GenUtils;
import com.mdframe.forge.plugin.generator.util.VelocityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenTableServiceImpl extends ServiceImpl<GenTableMapper, GenTable> implements IGenTableService {

    private final GenTableMapper genTableMapper;
    private final GenTableColumnMapper genTableColumnMapper;
    private final GeneratorConfig generatorConfig;
    private final IGenDatasourceService genDatasourceService;

    @Override
    public List<GenTable> selectDbTableList() {
        return genTableMapper.selectDbTableList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importGenTable(List<String> tableNames) {
        importGenTable(null, tableNames);
    }

    /**
     * 导入表结构（支持指定数据源）
     */
    @Transactional(rollbackFor = Exception.class)
    public void importGenTable(Long datasourceId, List<String> tableNames) {
        // 如果未指定数据源，使用默认数据源
        if (datasourceId == null) {
            GenDatasource defaultDatasource = genDatasourceService.getDefaultDatasource();
            datasourceId = defaultDatasource != null ? defaultDatasource.getDatasourceId() : null;
        }

        for (String tableName : tableNames) {
            // 查询表信息
            GenTable genTable;
            List<GenTableColumn> columns;

            if (datasourceId != null) {
                // 从指定数据源查询
                genTable = genDatasourceService.selectDbTableByName(datasourceId, tableName);
                if (genTable == null) {
                    throw new RuntimeException("表 " + tableName + " 不存在");
                }
                genTable.setDatasourceId(datasourceId);
                columns = genDatasourceService.selectDbTableColumnsByName(datasourceId, tableName);
            } else {
                // 使用默认数据源
                genTable = genTableMapper.selectDbTableByName(tableName);
                if (genTable == null) {
                    throw new RuntimeException("表 " + tableName + " 不存在");
                }
                columns = genTableColumnMapper.selectDbTableColumnsByName(tableName);
            }

            // 初始化表信息
            GenUtils.initTable(genTable, generatorConfig);

            // 如果存在先删除
            GenTable existTable = genTableMapper.selectOne(
                new LambdaQueryWrapper<GenTable>()
                    .eq(GenTable::getDatasourceId, datasourceId)
                    .eq(GenTable::getTableName, tableName)
            );
            if (existTable != null) {
                genTableMapper.deleteById(existTable.getTableId());
                genTableColumnMapper.delete(
                    new LambdaQueryWrapper<GenTableColumn>()
                        .eq(GenTableColumn::getTableId, existTable.getTableId())
                );
            }

            genTableMapper.insert(genTable);

            // 插入列信息
            for (GenTableColumn column : columns) {
                column.setTableId(genTable.getTableId());
                genTableColumnMapper.insert(column);
            }
        }
    }

    @Override
    public byte[] generatorCode(String tableName) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);

        try {
            // 查询表信息
            GenTable table = getGenTableByName(tableName);
            
            // 初始化Velocity
            VelocityUtils.initVelocity();
            
            // 准备上下文
            VelocityContext context = VelocityUtils.prepareContext(table);
            
            // 获取模板列表
            List<String> templates = VelocityUtils.getTemplateList();
            
            for (String template : templates) {
                // 渲染模板
                String code = VelocityUtils.renderTemplate(template, context);
                
                // 获取文件名
                String fileName = VelocityUtils.getFileName(template, table);
                if (fileName != null) {
                    // 添加到zip
                    zip.putNextEntry(new ZipEntry(fileName));
                    zip.write(code.getBytes("UTF-8"));
                    zip.closeEntry();
                }
            }
            
            zip.finish();
            zip.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("生成代码失败", e);
            throw new RuntimeException("生成代码失败: " + e.getMessage());
        } finally {
            IoUtil.close(zip);
            IoUtil.close(outputStream);
        }
    }

    @Override
    public byte[] batchGeneratorCode(String[] tableNames) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);

        try {
            VelocityUtils.initVelocity();
            
            for (String tableName : tableNames) {
                GenTable table = getGenTableByName(tableName);
                VelocityContext context = VelocityUtils.prepareContext(table);
                List<String> templates = VelocityUtils.getTemplateList();
                
                for (String template : templates) {
                    String code = VelocityUtils.renderTemplate(template, context);
                    String fileName = VelocityUtils.getFileName(template, table);
                    if (fileName != null) {
                        zip.putNextEntry(new ZipEntry(fileName));
                        zip.write(code.getBytes("UTF-8"));
                        zip.closeEntry();
                    }
                }
            }
            
            zip.finish();
            zip.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("批量生成代码失败", e);
            throw new RuntimeException("批量生成代码失败: " + e.getMessage());
        } finally {
            IoUtil.close(zip);
            IoUtil.close(outputStream);
        }
    }

    @Override
    public Map<String, String> previewCode(String tableName) {
        Map<String, String> dataMap = new LinkedHashMap<>();
        
        try {
            GenTable table = getGenTableByName(tableName);
            VelocityUtils.initVelocity();
            VelocityContext context = VelocityUtils.prepareContext(table);
            List<String> templates = VelocityUtils.getTemplateList();
            
            for (String template : templates) {
                String code = VelocityUtils.renderTemplate(template, context);
                String fileName = VelocityUtils.getFileName(template, table);
                if (fileName != null) {
                    dataMap.put(fileName, code);
                }
            }
        } catch (Exception e) {
            log.error("预览代码失败", e);
            throw new RuntimeException("预览代码失败: " + e.getMessage());
        }
        
        return dataMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGenTable(GenTable genTable) {
        // 更新表信息
        genTableMapper.updateById(genTable);
        
        // 更新列信息
        if (genTable.getColumns() != null && !genTable.getColumns().isEmpty()) {
            for (GenTableColumn column : genTable.getColumns()) {
                genTableColumnMapper.updateById(column);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGenTableByIds(Long[] tableIds) {
        // 删除表配置
        genTableMapper.deleteBatchIds(Arrays.asList(tableIds));
        
        // 删除列配置
        for (Long tableId : tableIds) {
            genTableColumnMapper.delete(
                new LambdaQueryWrapper<GenTableColumn>()
                    .eq(GenTableColumn::getTableId, tableId)
            );
        }
    }

    /**
     * 根据表名获取表信息（含列信息）
     */
    private GenTable getGenTableByName(String tableName) {
        GenTable genTable = genTableMapper.selectOne(
            new LambdaQueryWrapper<GenTable>()
                .eq(GenTable::getTableName, tableName)
        );
        
        if (genTable == null) {
            throw new RuntimeException("表配置不存在: " + tableName);
        }
        
        // 查询列信息
        List<GenTableColumn> columns = genTableColumnMapper.selectList(
            new LambdaQueryWrapper<GenTableColumn>()
                .eq(GenTableColumn::getTableId, genTable.getTableId())
                .orderByAsc(GenTableColumn::getSort)
        );
        
        genTable.setColumns(columns);
        genTable.setPkColumn(GenUtils.getPkColumn(columns));
        
        return genTable;
    }
}
