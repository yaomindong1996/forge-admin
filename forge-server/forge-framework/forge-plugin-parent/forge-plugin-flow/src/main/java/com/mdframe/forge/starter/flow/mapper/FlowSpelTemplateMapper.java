package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowSpelTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SPEL表达式模板Mapper
 *
 * @author forge
 */
@Mapper
public interface FlowSpelTemplateMapper extends BaseMapper<FlowSpelTemplate> {

    IPage<FlowSpelTemplate> selectTenantPage(Page<FlowSpelTemplate> page,
                                              @Param("tenantId") Long tenantId,
                                              @Param("templateName") String templateName,
                                              @Param("category") String category,
                                              @Param("status") Integer status);

    FlowSpelTemplate selectByIdAndTenant(@Param("id") Long id, @Param("tenantId") Long tenantId);

    int countByCodeAndTenant(@Param("templateCode") String templateCode, @Param("tenantId") Long tenantId,
                             @Param("excludeId") Long excludeId);

    int updateByIdAndTenant(@Param("template") FlowSpelTemplate template, @Param("tenantId") Long tenantId);

    int updateStatusByIdAndTenant(@Param("id") Long id, @Param("tenantId") Long tenantId,
                                  @Param("status") Integer status);

    int logicallyDeleteByIdAndTenant(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 查询启用状态的模板列表
     *
     * @param tenantId 租户ID
     * @return 模板列表
     */
    List<FlowSpelTemplate> selectEnabledList(@Param("tenantId") Long tenantId);
}
