package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 流程模型 Mapper
 */
@Mapper
public interface FlowModelMapper extends BaseMapper<FlowModel> {

    /**
     * 分页查询流程模型（支持父级分类查询子级数据）
     */
    IPage<FlowModel> selectModelPage(Page<FlowModel> page, @Param("modelName") String modelName,
                                       @Param("category") String category, @Param("status") Integer status);
}