package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.job.flow.JobFlowBindingSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 流程模型 Mapper
 */
@Mapper
public interface FlowModelMapper extends BaseMapper<FlowModel> {

    /** 按主键和租户读取未删除模型，供 @IgnoreTenant 控制器使用。 */
    FlowModel selectByIdAndTenant(@Param("id") String id,
                                  @Param("tenantId") Long tenantId);

    /**
     * 分页查询流程模型（支持父级分类查询子级数据）
     */
    IPage<FlowModel> selectModelPage(Page<FlowModel> page, @Param("modelName") String modelName,
                                      @Param("category") String category, @Param("status") Integer status,
                                      @Param("createBy") String createBy,
                                      @Param("tenantId") Long tenantId);

    /**
     * 按状态统计流程模型数量
     */
    Map<String, Object> selectStatusStatistics(@Param("modelName") String modelName,
                                               @Param("category") String category,
                                               @Param("createBy") String createBy,
                                               @Param("tenantId") Long tenantId);

    /** 按租户和流程定义 Key 查询未删除模型。 */
    FlowModel selectByModelKeyAndTenantId(@Param("modelKey") String modelKey,
                                          @Param("tenantId") Long tenantId);

    /** 按租户检查未删除模型 Key。 */
    long countByModelKeyAndTenantId(@Param("modelKey") String modelKey,
                                    @Param("tenantId") Long tenantId,
                                    @Param("excludeId") String excludeId);

    /**
     * 查询当前租户已发布流程模型目录。
     */
    List<FlowModel> selectEnabledModels(@Param("tenantId") Long tenantId,
                                        @Param("category") String category);

    /** 锁定当前租户内待排序模型，防止并发排序覆盖其他请求。 */
    List<FlowModel> selectByIdsForUpdate(@Param("ids") List<String> ids,
                                         @Param("tenantId") Long tenantId);

    /** 租户限定更新单个模型排序值。 */
    int updateSortOrder(@Param("id") String id,
                        @Param("tenantId") Long tenantId,
                        @Param("sortOrder") Integer sortOrder,
                        @Param("lastUpdateBy") String lastUpdateBy);

    JobFlowBindingSnapshot selectPublishedJobBinding(
            @Param("tenantId") Long tenantId,
            @Param("modelKey") String modelKey,
            @Param("modelVersion") Integer modelVersion);
}
