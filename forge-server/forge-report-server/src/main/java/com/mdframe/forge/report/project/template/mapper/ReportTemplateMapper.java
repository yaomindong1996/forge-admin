package com.mdframe.forge.report.project.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.report.project.template.domain.ReportTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Go-View 模板 Mapper
 */
@Mapper
public interface ReportTemplateMapper extends BaseMapper<ReportTemplate> {

    /**
     * 分页查询我的模板
     */
    Page<ReportTemplate> selectMyTemplatePage(Page<ReportTemplate> page,
                                              @Param("templateName") String templateName,
                                              @Param("publishStatus") String publishStatus,
                                              @Param("ownerId") Long ownerId);

    /**
     * 分页查询模板市场
     */
    Page<ReportTemplate> selectTemplateMarketPage(Page<ReportTemplate> page,
                                                  @Param("templateName") String templateName);

    /**
     * 查询模板详情
     */
    ReportTemplate selectTemplateDetail(@Param("id") Long id);

    /**
     * 查询模板详情，包含已逻辑删除记录。
     */
    ReportTemplate selectTemplateByIdIncludingDeleted(@Param("id") Long id);

    /**
     * 恢复已逻辑删除模板。
     */
    int restoreTemplateById(@Param("id") Long id);
}
