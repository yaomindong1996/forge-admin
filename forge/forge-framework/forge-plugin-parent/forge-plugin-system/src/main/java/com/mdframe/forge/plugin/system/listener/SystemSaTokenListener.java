package com.mdframe.forge.plugin.system.listener;

import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.SaLoginModel;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysOnlineUserService;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.auth.service.ILoginLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @date 2025/12/15
 */
@Component
@Slf4j
public class SystemSaTokenListener implements SaTokenListener {
    
    @Autowired
    private ISysUserService sysUserService;
    
    @Autowired
    private ILoginLockService iLoginLockService;
    
    @Autowired
    private ISysOnlineUserService onlineUserService;
    
    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
        log.info("用户登录成功 - loginType: {}, loginId: {}, tokenValue: {}", loginType, loginId, tokenValue);
        
        try {
            // 记录在线用户信息
            onlineUserService.addOnlineUser(tokenValue, loginId);
        } catch (Exception e) {
            log.error("记录在线用户失败", e);
        }
    }
    
    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        log.info("用户登出 - loginType: {}, loginId: {}, tokenValue: {}", loginType, loginId, tokenValue);
        
        try {
            // 移除在线用户记录
            onlineUserService.removeOnlineUser(tokenValue);
        } catch (Exception e) {
            log.error("移除在线用户失败", e);
        }
    }
    
    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        log.info("用户被踢下线 - loginType: {}, loginId: {}, tokenValue: {}", loginType, loginId, tokenValue);
        
        try {
            // 通知用户被踢下线
            onlineUserService.notifyUserKickout(tokenValue, loginId);
            // 移除在线用户记录
            onlineUserService.removeOnlineUser(tokenValue);
        } catch (Exception e) {
            log.error("处理用户被踢下线失败", e);
        }
    }
    
    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        log.info("用户被顶下线 - loginType: {}, loginId: {}, tokenValue: {}", loginType, loginId, tokenValue);
        
        try {
            // 通知用户被顶下线
            onlineUserService.notifyUserReplaced(tokenValue, loginId);
            // 移除在线用户记录
            onlineUserService.removeOnlineUser(tokenValue);
        } catch (Exception e) {
            log.error("处理用户被顶下线失败", e);
        }
    }
    
    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
        log.info("用户被封禁 - loginType: {}, loginId: {}, service: {}, level: {}, disableTime: {}",
                loginType, loginId, service, level, disableTime);
        
        try {
            // 通知用户被封禁
            onlineUserService.notifyUserBanned(loginId, disableTime);
        } catch (Exception e) {
            log.error("处理用户被封禁失败", e);
        }
        sysUserService.lambdaUpdate()
                .eq(SysUser::getId, loginId)
                .set(SysUser::getUserStatus, SystemConstants.UserStatus.LOCKED)
                .set(SysUser::getUpdateTime, LocalDateTime.now()).update();
    }
    
    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
        sysUserService.lambdaUpdate()
                .eq(SysUser::getId, loginId)
                .set(SysUser::getUserStatus, SystemConstants.UserStatus.UNLOCKED)
                .set(SysUser::getUpdateTime, LocalDateTime.now()).update();
        iLoginLockService.unlock(Long.parseLong(String.valueOf(loginId)));
    }
    
    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {
    
    }
    
    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {
    
    }
    
    @Override
    public void doCreateSession(String id) {
    
    }
    
    @Override
    public void doLogoutSession(String id) {
    
    }
    
    @Override
    public void doRenewTimeout(String tokenValue, Object loginId, long timeout) {
        log.debug("Token续期 - tokenValue: {}, loginId: {}, timeout: {}", tokenValue, loginId, timeout);
        
        try {
            // 更新最后活动时间
            onlineUserService.updateLastActivityTime(tokenValue);
        } catch (Exception e) {
            log.error("更新最后活动时间失败", e);
        }
    }
}
