package com.mdframe.forge.plugin.generator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.domain.entity.GenTemplate;
import com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper;
import com.mdframe.forge.plugin.generator.mapper.GenTableMapper;
import com.mdframe.forge.plugin.generator.mapper.GenTemplateMapper;
import com.mdframe.forge.plugin.generator.service.IGenTemplateService;
import com.mdframe.forge.plugin.generator.util.GenUtils;
import com.mdframe.forge.plugin.generator.util.VelocityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * 代码生成模板配置Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenTemplateServiceImpl extends ServiceImpl<GenTemplateMapper, GenTemplate> implements IGenTemplateService {

    private final GenTemplateMapper genTemplateMapper;
    private final GenTableMapper genTableMapper;
    private final GenTableColumnMapper genTableColumnMapper;

    /**
     * 模板类型枚举
     */
    private static final List<String> TEMPLATE_TYPES = Arrays.asList(
            "ENTITY", "MAPPER", "MAPPER_XML", "SERVICE", "SERVICE_IMPL",
            "CONTROLLER", "DTO", "VO", "QUERY", "SQL"
    );

    @Override
    public List<GenTemplate> listByEngine(String engine) {
        return genTemplateMapper.selectList(
                new LambdaQueryWrapper<GenTemplate>()
                        .eq(GenTemplate::getTemplateEngine, engine)
                        .eq(GenTemplate::getIsEnabled, 1)
                        .orderByAsc(GenTemplate::getSort)
        );
    }

    @Override
    public String previewTemplate(Long templateId, Long tableId) {
        // 查询模板
        GenTemplate template = genTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        // 查询表信息
        GenTable table = genTableMapper.selectById(tableId);
        if (table == null) {
            throw new RuntimeException("表配置不存在");
        }

        // 查询列信息
        List<GenTableColumn> columns = genTableColumnMapper.selectList(
                new LambdaQueryWrapper<GenTableColumn>()
                        .eq(GenTableColumn::getTableId, tableId)
                        .orderByAsc(GenTableColumn::getSort)
        );
        table.setColumns(columns);
        table.setPkColumn(GenUtils.getPkColumn(columns));

        // 初始化 Velocity
        VelocityUtils.initVelocity();

        // 准备上下文
        VelocityContext context = VelocityUtils.prepareContext(table);

        // 渲染模板内容
        try {
            StringWriter writer = new StringWriter();
            org.apache.velocity.app.Velocity.evaluate(context, writer, "template", template.getTemplateContent());
            return writer.toString();
        } catch (Exception e) {
            log.error("模板渲染失败: templateId={}, tableId={}", templateId, tableId, e);
            throw new RuntimeException("模板渲染失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> listTemplateTypes() {
        return TEMPLATE_TYPES;
    }
}
