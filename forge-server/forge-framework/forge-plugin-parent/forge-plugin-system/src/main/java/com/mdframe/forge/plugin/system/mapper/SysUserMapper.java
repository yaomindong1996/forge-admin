package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户Mapper接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询用户列表
     */
    IPage<SysUser> selectUserPage(Page<SysUser> page, @Param("query") SysUserQuery query);

    /**
     * 导出查询用户列表（不分页）
     */
    List<SysUser> selectExportList(@Param("query") SysUserQuery query);

    /**
     * 登录时按用户名和当前租户查询用户
     */
    SysUser selectByUsernameForLogin(@Param("username") String username, @Param("tenantId") Long tenantId);

    /**
     * 登录消歧：按用户名查出全部未删除账号（同一用户名可存在于不同租户）。
     */
    List<SysUser> selectUsersByUsernameForLogin(@Param("username") String username);

    /**
     * 登录时按手机号和当前租户查询用户
     */
    SysUser selectByPhoneForLogin(@Param("phone") String phone, @Param("tenantId") Long tenantId);

    /**
     * 外部身份首次映射时按已验证手机号查询有效候选，最多返回两条用于冲突检测。
     */
    List<SysUser> selectEligibleUsersByVerifiedPhone(@Param("phone") String phone,
                                                      @Param("tenantId") Long tenantId);

    /**
     * 登录时按邮箱和当前租户查询用户
     */
    SysUser selectByEmailForLogin(@Param("email") String email, @Param("tenantId") Long tenantId);

    /**
     * 协同目录同步：按手机号/邮箱查询租户内未删除用户（身份冲突检测，仅限本租户）
     */
    List<SysUser> selectActiveUsersByPhoneOrEmail(@Param("tenantId") Long tenantId,
                                                  @Param("phone") String phone,
                                                  @Param("email") String email);

    /**
     * 协同目录同步：仅更新同步拥有的资料字段，不触碰密码/状态/角色等本地资产
     */
    int updateUserProfileBySync(@Param("tenantId") Long tenantId,
                                @Param("userId") Long userId,
                                @Param("realName") String realName,
                                @Param("avatar") String avatar,
                                @Param("email") String email,
                                @Param("phone") String phone);

    /** 流程运行时按租户读取已启用用户及其主组织/岗位摘要。 */
    Map<String, Object> selectFlowUserInfo(@Param("tenantId") Long tenantId,
                                           @Param("userId") Long userId);

    /** 流程图和历史详情批量读取用户及主组织/岗位摘要。 */
    List<Map<String, Object>> selectFlowUserInfoBatch(@Param("tenantId") Long tenantId,
                                                      @Param("userIds") List<Long> userIds);

    /** 流程选人列表使用单次 SQL 返回组织摘要，避免逐用户回查组织。 */
    List<Map<String, Object>> selectFlowUsers(@Param("tenantId") Long tenantId,
                                              @Param("keyword") String keyword,
                                              @Param("orgId") Long orgId);

    List<Long> selectFlowUserIdsByOrg(@Param("tenantId") Long tenantId,
                                      @Param("orgId") Long orgId);

    List<Long> selectFlowUserIdsByPost(@Param("tenantId") Long tenantId,
                                       @Param("postId") Long postId);

    List<Long> selectFlowUserIdsByOrgAndPost(@Param("tenantId") Long tenantId,
                                             @Param("orgId") Long orgId,
                                             @Param("postId") Long postId);

    List<Long> selectFlowUserIdsByRegion(@Param("tenantId") Long tenantId,
                                         @Param("regionCode") String regionCode);

    /** 校验流程用户组成员是否均为当前租户内启用用户。 */
    List<Long> selectFlowAvailableUserIds(@Param("tenantId") Long tenantId,
                                          @Param("userIds") List<Long> userIds);
}
