package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowCategory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 流程分类 Mapper
 */
@Mapper
public interface FlowCategoryMapper extends BaseMapper<FlowCategory> {

    /** 管理页全量分类（含停用）。 */
    List<FlowCategory> selectManageList();
}