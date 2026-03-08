package com.mdframe.forge.starter.auth.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.hutool.extra.servlet.ServletUtil;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Sa-Token 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class SaTokenExceptionHandler {

    /**
     * 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public RespInfo<Void> handleNotLoginException(NotLoginException e) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.error("未登录异常：{}，请求地址：{}", e.getMessage(), request.getRequestURI());
        }
        log.error("未登录异常：", e);
        String message = "未登录或登录已过期，请重新登录";
        
        // 根据不同的场景细化提示
        if (e.getType().equals(NotLoginException.NOT_TOKEN)) {
            message = "未提供登录凭证";
        } else if (e.getType().equals(NotLoginException.INVALID_TOKEN)) {
            message = "登录凭证无效";
        } else if (e.getType().equals(NotLoginException.TOKEN_TIMEOUT)) {
            message = "登录已过期";
        } else if (e.getType().equals(NotLoginException.BE_REPLACED)) {
            message = "您的账号已在其他地方登录";
        } else if (e.getType().equals(NotLoginException.KICK_OUT)) {
            message = "您已被强制下线";
        }
        
        return RespInfo.error(401, message);
    }

    /**
     * 缺少权限异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public RespInfo<Void> handleNotPermissionException(NotPermissionException e) {
        log.error("权限不足异常：{}", e.getMessage());
        return RespInfo.error(403, "权限不足，无法访问");
    }

    /**
     * 缺少角色异常
     */
    @ExceptionHandler(NotRoleException.class)
    public RespInfo<Void> handleNotRoleException(NotRoleException e) {
        log.error("角色不足异常：{}", e.getMessage());
        return RespInfo.error(403, "角色权限不足，无法访问");
    }

    /**
     * 账号锁定异常
     */
    @ExceptionHandler(AccountLockedException.class)
    public RespInfo<Void> handleAccountLockedException(AccountLockedException e) {
        log.warn("账号锁定异常：{}", e.getMessage());
        return RespInfo.error(423, e.getMessage());
    }
}
