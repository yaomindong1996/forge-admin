package com.mdframe.forge.plugin.generator.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper;
import com.mdframe.forge.plugin.generator.util.GenUtils;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代码生成表字段配置Controller
 */
@RestController
@RequestMapping("/generator/column")
@RequiredArgsConstructor
public class GenTableColumnController {

    private final GenTableColumnMapper genTableColumnMapper;

    /**
     * 查询表字段列表
     */
    @GetMapping("/list/{tableId}")
    public RespInfo<List<GenTableColumn>> list(@PathVariable Long tableId) {
        List<GenTableColumn> columns = genTableColumnMapper.selectList(
                new LambdaQueryWrapper<GenTableColumn>()
                        .eq(GenTableColumn::getTableId, tableId)
                        .orderByAsc(GenTableColumn::getSort)
        );
        return RespInfo.success(columns);
    }

    /**
     * 批量更新字段配置
     */
    @PostMapping("/batchUpdate")
    @OperationLog(module = "字段配置", type = OperationType.UPDATE, desc = "批量更新字段配置")
    @Transactional(rollbackFor = Exception.class)
    public RespInfo<Void> batchUpdate(@RequestBody List<GenTableColumn> columns) {
        if (columns == null || columns.isEmpty()) {
            return RespInfo.error("字段列表不能为空");
        }
        
        for (int i = 0; i < columns.size(); i++) {
            GenTableColumn column = columns.get(i);
            // 更新排序
            column.setSort(i);
            genTableColumnMapper.updateById(column);
        }
        
        return RespInfo.success();
    }

    /**
     * 重置字段配置为默认值
     */
    @PostMapping("/resetConfig/{tableId}")
    @OperationLog(module = "字段配置", type = OperationType.UPDATE, desc = "重置字段配置")
    @Transactional(rollbackFor = Exception.class)
    public RespInfo<Void> resetConfig(@PathVariable Long tableId) {
        List<GenTableColumn> columns = genTableColumnMapper.selectList(
                new LambdaQueryWrapper<GenTableColumn>()
                        .eq(GenTableColumn::getTableId, tableId)
        );
        
        for (GenTableColumn column : columns) {
            // 重置为默认配置
            GenUtils.initColumnField(column);
            genTableColumnMapper.updateById(column);
        }
        
        return RespInfo.success();
    }
}
