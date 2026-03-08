package com.mdframe.forge.plugin.generator.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.service.IGenDatasourceService;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代码生成器数据源管理Controller
 */
@RestController
@RequestMapping("/generator/datasource")
@RequiredArgsConstructor
public class GenDatasourceController {

    private final IGenDatasourceService genDatasourceService;

    /**
     * 分页查询数据源列表
     */
    @GetMapping("/list")
    @OperationLog(module = "数据源管理", type = OperationType.QUERY, desc = "分页查询数据源列表")
    public RespInfo<Page<GenDatasource>> list(PageQuery pageQuery, String datasourceName) {
        LambdaQueryWrapper<GenDatasource> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(datasourceName), GenDatasource::getDatasourceName, datasourceName)
                .orderByAsc(GenDatasource::getSort)
                .orderByDesc(GenDatasource::getCreateTime);
        Page<GenDatasource> page = genDatasourceService.page(pageQuery.toPage(), wrapper);
        return RespInfo.success(page);
    }

    /**
     * 查询所有启用的数据源
     */
    @GetMapping("/enabled")
    public RespInfo<List<GenDatasource>> enabledList() {
        List<GenDatasource> list = genDatasourceService.list(
            new LambdaQueryWrapper<GenDatasource>()
                .eq(GenDatasource::getIsEnabled, 1)
                .orderByAsc(GenDatasource::getSort)
        );
        return RespInfo.success(list);
    }

    /**
     * 根据ID查询数据源详情
     */
    @GetMapping("/{datasourceId}")
    public RespInfo<GenDatasource> getInfo(@PathVariable Long datasourceId) {
        GenDatasource datasource = genDatasourceService.getById(datasourceId);
        return RespInfo.success(datasource);
    }

    /**
     * 新增数据源
     */
    @PostMapping("/add")
    @OperationLog(module = "数据源管理", type = OperationType.ADD, desc = "新增数据源")
    public RespInfo<Void> add(@RequestBody GenDatasource datasource) {
        genDatasourceService.save(datasource);
        return RespInfo.success();
    }

    /**
     * 修改数据源
     */
    @PostMapping("/edit")
    @OperationLog(module = "数据源管理", type = OperationType.UPDATE, desc = "修改数据源")
    public RespInfo<Void> edit(@RequestBody GenDatasource datasource) {
        genDatasourceService.updateById(datasource);
        return RespInfo.success();
    }

    /**
     * 删除数据源
     */
    @PostMapping("/remove/{datasourceId}")
    @OperationLog(module = "数据源管理", type = OperationType.DELETE, desc = "删除数据源")
    public RespInfo<Void> remove(@PathVariable Long datasourceId) {
        genDatasourceService.removeById(datasourceId);
        return RespInfo.success();
    }

    /**
     * 测试数据源连接
     */
    @PostMapping("/test/{datasourceId}")
    @OperationLog(module = "数据源管理", type = OperationType.OTHER, desc = "测试数据源连接")
    public RespInfo<Void> testConnection(@PathVariable Long datasourceId) {
        boolean success = genDatasourceService.testConnection(datasourceId);
        return success ? RespInfo.success() : RespInfo.error("连接失败");
    }

    /**
     * 查询指定数据源的表列表
     */
    @GetMapping("/{datasourceId}/tables")
    public RespInfo<List<GenTable>> getTables(@PathVariable Long datasourceId) {
        List<GenTable> tables = genDatasourceService.selectDbTableList(datasourceId);
        return RespInfo.success(tables);
    }
}
