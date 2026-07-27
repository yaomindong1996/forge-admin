package com.mdframe.forge.plugin.generator.controller;

import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.service.IGenTableColumnService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代码生成表字段配置Controller
 */
@RestController
@RequestMapping("/generator/column")
@RequiredArgsConstructor
@ApiEncrypt
@ApiDecrypt
public class GenTableColumnController {

    private final IGenTableColumnService genTableColumnService;

    /**
     * 根据数据库表名查询表字段信息（直接查 information_schema）
     */
    @GetMapping("/db/{tableName}")
    public RespInfo<List<GenTableColumn>> listByTableName(@PathVariable String tableName) {
        return RespInfo.success(genTableColumnService.selectDbTableColumnsByName(tableName));
    }

    /**
     * 查询表字段列表
     */
    @GetMapping("/list/{tableId}")
    public RespInfo<List<GenTableColumn>> list(@PathVariable Long tableId) {
        return RespInfo.success(genTableColumnService.selectTableColumns(tableId));
    }

    /**
     * 批量更新字段配置
     */
    @PostMapping("/batchUpdate")
    @OperationLog(module = "字段配置", type = OperationType.UPDATE, desc = "批量更新字段配置")
    public RespInfo<Void> batchUpdate(@RequestBody List<GenTableColumn> columns) {
        genTableColumnService.batchUpdate(columns);
        return RespInfo.success();
    }

    /**
     * 重置字段配置为默认值
     */
    @PostMapping("/resetConfig/{tableId}")
    @OperationLog(module = "字段配置", type = OperationType.UPDATE, desc = "重置字段配置")
    public RespInfo<Void> resetConfig(@PathVariable Long tableId) {
        genTableColumnService.resetConfig(tableId);
        return RespInfo.success();
    }
}
