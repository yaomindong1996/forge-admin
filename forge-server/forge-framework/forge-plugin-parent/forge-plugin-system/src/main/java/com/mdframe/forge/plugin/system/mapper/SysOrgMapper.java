package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysOrgQuery;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.vo.SysOrgTreeVO;
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

    /**
     * 按租户查询组织及其所有子组织，供流程等跨模块解析使用。
     * 复杂层级查询必须在 Mapper XML 中完成，并显式绑定租户。
     */
    List<Long> selectOrgAndChildrenIdsByTenant(@Param("orgId") Long orgId,
                                               @Param("tenantId") Long tenantId);

    /**
     * 查询直接子组织数量。
     */
    Long countChildOrgs(@Param("orgId") Long orgId);

    /**
     * 查询组织用户绑定数量。
     */
    Long countUserOrgBindings(@Param("orgId") Long orgId);
    
    List<SysOrgTreeVO> selectOrgLazyTree(@Param("query") SysOrgQuery query);
    
    List<SysOrgTreeVO> selectOrgChildrenByParentId(@Param("parentId") Long parentId, @Param("tenantId") Long tenantId);

    /**
     * 按组织ID集合查询当前租户内启用组织。
     */
    List<SysOrg> selectEnabledOrgsByIds(@Param("tenantId") Long tenantId, @Param("orgIds") List<Long> orgIds);

    /** 流程运行时按租户读取单个启用组织，避免通过 Service 回查跨租户数据。 */
    SysOrg selectFlowOrgById(@Param("tenantId") Long tenantId, @Param("orgId") Long orgId);

    /** 流程选人组织树使用的租户限定有效组织列表。 */
    List<SysOrg> selectFlowOrgList(@Param("tenantId") Long tenantId);

    /**
     * 协同目录同步：仅更新同步拥有的组织字段（名称/父级/层级/排序），不触碰负责人等手工资产
     */
    int updateOrgSyncFields(@Param("tenantId") Long tenantId,
                            @Param("orgId") Long orgId,
                            @Param("orgName") String orgName,
                            @Param("parentId") Long parentId,
                            @Param("ancestors") String ancestors,
                            @Param("sort") Integer sort);
}
