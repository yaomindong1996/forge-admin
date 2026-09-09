package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysPostQuery;
import com.mdframe.forge.plugin.system.entity.SysPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 岗位Mapper接口
 */
@Mapper
public interface SysPostMapper extends BaseMapper<SysPost> {

    /**
     * 分页查询岗位列表（带所属组织名称）
     */
    IPage<SysPost> selectPostPage(Page<SysPost> page, @Param("query") SysPostQuery query);

    /**
     * 查询岗位列表（带所属组织名称）
     */
    List<SysPost> selectPostList(@Param("query") SysPostQuery query);

    /** 流程选人岗位列表使用的租户限定有效岗位。 */
    List<SysPost> selectFlowPosts(@Param("tenantId") Long tenantId,
                                  @Param("orgId") Long orgId);
}
