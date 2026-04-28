package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysOrgQuery;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 组织Mapper接口
 */
@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrg> {

    /**
     * 分页查询组织列表
     */
    IPage<SysOrg> selectOrgPage(Page<SysOrg> page, @Param("query") SysOrgQuery query);

    /**
     * 查询组织列表（用于构建树）
     */
    List<SysOrg> selectOrgList(@Param("query") SysOrgQuery query);

    /**
     * 查询组织及其子组织ID列表
     */
    List<Long> selectOrgAndChildrenIds(@Param("orgId") Long orgId);
}
