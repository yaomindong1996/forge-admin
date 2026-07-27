package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.GenTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 代码生成模板配置Mapper接口
 */
@Mapper
public interface GenTemplateMapper extends BaseMapper<GenTemplate> {

    Page<GenTemplate> selectTemplatePage(Page<GenTemplate> page,
                                          @Param("templateName") String templateName,
                                          @Param("templateType") String templateType,
                                          @Param("templateEngine") String templateEngine);

    List<GenTemplate> selectEnabledTemplates(@Param("templateEngine") String templateEngine);
}
