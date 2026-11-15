package com.mdframe.forge.plugin.system.mapper;

import com.mdframe.forge.plugin.system.auth.UserPasswordCredential;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户密码凭证 Mapper：sys_user 的凭证列与 sys_user_password_history。
 * 调用方需在忽略租户的上下文中调用，租户条件由 SQL 显式携带。
 */
@Mapper
public interface SysUserCredentialMapper {

    /** 读取用户当前凭证快照，用户不存在或已删除时返回 null。 */
    UserPasswordCredential selectCredential(@Param("userId") Long userId);

    /** 写入新密码哈希，同时递增凭证版本并刷新密码变更时间。 */
    int updateCredential(@Param("userId") Long userId,
                         @Param("encodedPassword") String encodedPassword,
                         @Param("forcePasswordChange") boolean forcePasswordChange,
                         @Param("changedTime") LocalDateTime changedTime);

    /** 最近 limit 个历史密码哈希，按替换时间倒序。 */
    List<String> selectRecentPasswordHashes(@Param("tenantId") Long tenantId,
                                            @Param("userId") Long userId,
                                            @Param("limit") int limit);

    /** 记录一条被替换下来的密码哈希。 */
    int insertPasswordHistory(@Param("tenantId") Long tenantId,
                              @Param("userId") Long userId,
                              @Param("passwordHash") String passwordHash,
                              @Param("changedTime") LocalDateTime changedTime);

    /** 用户全部历史记录 id，按替换时间倒序。 */
    List<Long> selectHistoryIds(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /** 按 id 删除历史记录。 */
    int deleteHistoryByIds(@Param("tenantId") Long tenantId,
                           @Param("userId") Long userId,
                           @Param("ids") List<Long> ids);
}
