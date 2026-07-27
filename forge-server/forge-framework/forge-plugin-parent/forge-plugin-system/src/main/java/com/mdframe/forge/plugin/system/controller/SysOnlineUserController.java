package com.mdframe.forge.plugin.system.controller;


import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.constant.OnlineUserPermissions;
import com.mdframe.forge.plugin.system.entity.SysOnlineUser;
import com.mdframe.forge.plugin.system.service.ISysOnlineUserService;
import com.mdframe.forge.plugin.system.vo.SysOnlineUserVO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 在线用户管理控制器
 */
@RestController
@RequestMapping("/auth/online")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class SysOnlineUserController {

    private final ISysOnlineUserService onlineUserService;

    /**
     * 分页获取在线用户列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param username 用户名(可选,用于搜索)
     * @return 在线用户分页数据
     */
    @GetMapping("/page")
    @SaCheckPermission(OnlineUserPermissions.QUERY)
    public RespInfo<IPage<SysOnlineUserVO>> getOnlineUsersPage(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String username) {
        Page<SysOnlineUser> page = new Page<>(pageNum, pageSize);
        IPage<SysOnlineUser> result = onlineUserService.getOnlineUsersPage(page, username);
        return RespInfo.success(result.convert(SysOnlineUserVO::from));
    }

    /**
     * 获取在线用户列表（不分页）
     *
     * @param username 用户名(可选,用于搜索)
     * @return 在线用户列表
     */
    @GetMapping("/list")
    @SaCheckPermission(OnlineUserPermissions.QUERY)
    public RespInfo<List<SysOnlineUserVO>> getOnlineUsers(@RequestParam(required = false) String username) {
        List<SysOnlineUser> sysOnlineUsers = onlineUserService.getOnlineUsers(username);
        return RespInfo.success(sysOnlineUsers.stream().map(SysOnlineUserVO::from).toList());
    }

    /**
     * 强制用户下线
     *
     * @param sessionId 在线会话记录ID
     * @return 操作结果
     */
    @PostMapping("/kickout")
    @SaCheckPermission(OnlineUserPermissions.KICKOUT)
    public RespInfo<Void> kickoutUser(@RequestParam Long sessionId) {
        onlineUserService.kickoutSession(sessionId);
        return RespInfo.success();
    }

    /**
     * 批量强制用户下线
     *
     * @param sessionIds 在线会话记录ID列表
     * @return 操作结果
     */
    @PostMapping("/batchKickout")
    @SaCheckPermission(OnlineUserPermissions.BATCH_KICKOUT)
    public RespInfo<Void> batchKickoutUser(@RequestBody List<Long> sessionIds) {
        onlineUserService.batchKickoutSessions(sessionIds);
        return RespInfo.success();
    }

    /**
     * 封禁用户
     *
     * @param userId     用户ID
     * @param banSeconds 封禁时长(秒)
     * @param reason     封禁原因
     * @return 操作结果
     */
    @PostMapping("/ban")
    @SaCheckPermission(OnlineUserPermissions.BAN)
    public RespInfo<Void> banUser(@RequestParam Long userId,
                                   @RequestParam long banSeconds,
                                   @RequestParam(required = false) String reason) {
        onlineUserService.banUser(userId, banSeconds, reason);
        return RespInfo.success();
    }

    /**
     * 解封用户
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/unban")
    @SaCheckPermission(OnlineUserPermissions.UNBAN)
    public RespInfo<Void> unbanUser(@RequestParam Long userId) {
        onlineUserService.unbanUser(userId);
        return RespInfo.success();
    }

    /**
     * 获取用户的所有在线会话记录ID
     *
     * @param userId 用户ID
     * @return 在线会话记录ID列表
     */
    @GetMapping("/userTokens")
    @SaCheckPermission(OnlineUserPermissions.QUERY)
    public RespInfo<List<Long>> getUserSessionIds(@RequestParam Long userId) {
        return RespInfo.success(onlineUserService.getUserSessionIds(userId));
    }
}
