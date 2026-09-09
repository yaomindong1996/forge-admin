package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 流程审批节点配置Mapper
 */
@Mapper
public interface FlowNodeConfigMapper extends BaseMapper<FlowNodeConfig> {

    /**
     * 按模型 ID 或模型 Key 查询当前租户的节点配置。
     * 设计器接口历史上使用 modelId，运行时使用 modelKey，统一在 Mapper 层兼容两种引用。
     */
    java.util.List<FlowNodeConfig> selectByModelRef(@Param("modelRef") String modelRef,
                                                     @Param("tenantId") Long tenantId);

    FlowNodeConfig selectByIdAndTenant(@Param("id") String id,
                                       @Param("tenantId") Long tenantId);

    /**
     * 根据模型Key和节点ID查询节点配置。
     */
    FlowNodeConfig selectByModelKeyAndNode(@Param("modelKey") String modelKey,
                                           @Param("nodeId") String nodeId,
                                           @Param("tenantId") Long tenantId);

    /**
     * 使用当前字符串主键作为删除墓碑，避免同一模型节点只能保留一条删除历史。
     */
    int logicDeleteById(@Param("id") String id,
                        @Param("tenantId") Long tenantId);

    /**
     * 按模型批量写入各行自身主键作为删除墓碑。
     */
    int logicDeleteByModelId(@Param("modelId") String modelId,
                             @Param("tenantId") Long tenantId);
}
