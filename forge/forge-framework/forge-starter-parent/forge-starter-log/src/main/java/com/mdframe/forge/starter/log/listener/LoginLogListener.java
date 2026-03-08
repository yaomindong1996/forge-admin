package com.mdframe.forge.starter.log.listener;

import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.mdframe.forge.starter.core.context.LogProperties;
import com.mdframe.forge.starter.log.domain.LoginLogInfo;
import com.mdframe.forge.starter.log.service.ILogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;

/**
 * 登录日志监听器
 * 监听Sa-Token的登录、登出事件，记录登录日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(ILogService.class)
public class LoginLogListener implements SaTokenListener {

    private final ILogService logService;
    private final LogProperties logProperties;
    @Qualifier("logTaskExecutor")
    private final Executor logTaskExecutor;

    private static final DateTimeFormatter TRACE_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 每次登录时触发
     */
    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
        // 检查是否启用登录日志
        if (logProperties.getEnableLoginLog() == null || !logProperties.getEnableLoginLog()) {
            return;
        }

        try {
            String traceId = generateTraceId();
            MDC.put(TRACE_ID_KEY, traceId);
            
            LoginLogInfo logInfo = buildLoginLog(loginId, "LOGIN", 1, "登录成功");
            printLoginLog(logInfo);
            saveLoginLogAsync(logInfo);
            
            MDC.remove(TRACE_ID_KEY);
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 每次登出时触发
     */
    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        // 检查是否启用登录日志
        if (logProperties.getEnableLoginLog() == null || !logProperties.getEnableLoginLog()) {
            return;
        }

        try {
            String traceId = generateTraceId();
            MDC.put(TRACE_ID_KEY, traceId);
            
            LoginLogInfo logInfo = buildLoginLog(loginId, "LOGOUT", 1, "登出成功");
            printLoginLog(logInfo);
            saveLoginLogAsync(logInfo);
            
            MDC.remove(TRACE_ID_KEY);
        } catch (Exception e) {
            log.error("记录登出日志失败", e);
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 每次被踢下线时触发
     */
    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        try {
            String traceId = generateTraceId();
            MDC.put(TRACE_ID_KEY, traceId);
            
            LoginLogInfo logInfo = buildLoginLog(loginId, "KICKOUT", 1, "被踢下线");
            printLoginLog(logInfo);
            saveLoginLogAsync(logInfo);
            
            MDC.remove(TRACE_ID_KEY);
        } catch (Exception e) {
            log.error("记录踢下线日志失败", e);
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 每次被顶下线时触发
     */
    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        try {
            String traceId = generateTraceId();
            MDC.put(TRACE_ID_KEY, traceId);
            
            LoginLogInfo logInfo = buildLoginLog(loginId, "REPLACED", 1, "被顶下线");
            printLoginLog(logInfo);
            saveLoginLogAsync(logInfo);
            
            MDC.remove(TRACE_ID_KEY);
        } catch (Exception e) {
            log.error("记录顶下线日志失败", e);
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 每次被封禁时触发
     */
    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
        try {
            String traceId = generateTraceId();
            MDC.put(TRACE_ID_KEY, traceId);
            
            LoginLogInfo logInfo = buildLoginLog(loginId, "DISABLE", 0, "账号被封禁");
            printLoginLog(logInfo);
            saveLoginLogAsync(logInfo);
            
            MDC.remove(TRACE_ID_KEY);
        } catch (Exception e) {
            log.error("记录封禁日志失败", e);
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 每次被解封时触发
     */
    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
        try {
            String traceId = generateTraceId();
            MDC.put(TRACE_ID_KEY, traceId);
            
            LoginLogInfo logInfo = buildLoginLog(loginId, "UNTIE_DISABLE", 1, "账号解封");
            printLoginLog(logInfo);
            saveLoginLogAsync(logInfo);
            
            MDC.remove(TRACE_ID_KEY);
        } catch (Exception e) {
            log.error("记录解封日志失败", e);
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 每次打开二级认证时触发
     */
    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {
        // 不记录日志
    }

    /**
     * 每次创建Session时触发
     */
    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {
        // 不记录日志
    }

    /**
     * 每次创建Session时触发
     */
    @Override
    public void doCreateSession(String id) {
        // 不记录日志
    }

    /**
     * 每次注销Session时触发
     */
    @Override
    public void doLogoutSession(String id) {
        // 不记录日志
    }

    /**
     * 每次Token续期时触发
     */
    @Override
    public void doRenewTimeout(String tokenValue, Object loginId, long timeout) {
        // 不记录日志
    }

    /**
     * 生成traceId
     */
    private String generateTraceId() {
        String timestamp = LocalDateTime.now().format(TRACE_ID_FORMATTER);
        String random = String.valueOf(Math.abs(System.nanoTime() % 1000000));
        return timestamp + random + "_LOG";
    }

    /**
     * 构建登录日志对象
     */
    private LoginLogInfo buildLoginLog(Object loginId, String loginType, Integer loginStatus, String loginMessage) {
        LoginLogInfo logInfo = new LoginLogInfo();
        logInfo.setUserId(Long.valueOf(loginId.toString()));
        logInfo.setLoginType(loginType);
        logInfo.setLoginStatus(loginStatus);
        logInfo.setLoginMessage(loginMessage);
        logInfo.setLoginTime(LocalDateTime.now());

        // 获取请求信息
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // 获取IP
                logInfo.setLoginIp(getClientIp(request));

                // 解析User-Agent
                String userAgentStr = request.getHeader("User-Agent");
                logInfo.setUserAgent(userAgentStr);

                if (StrUtil.isNotBlank(userAgentStr)) {
                    UserAgent userAgent = UserAgentUtil.parse(userAgentStr);
                    logInfo.setBrowser(userAgent.getBrowser().getName() + " " + userAgent.getVersion());
                    logInfo.setOs(userAgent.getOs().getName());
                }
            }
        } catch (Exception e) {
            log.debug("获取请求信息失败", e);
        }

        return logInfo;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多IP的情况，取第一个
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 异步保存登录日志
     */
    private void saveLoginLogAsync(LoginLogInfo logInfo) {
        try {
            // 使用线程池异步保存日志，避免影响业务性能
            logTaskExecutor.execute(() -> {
                try {
                    logService.saveLoginLog(logInfo);
                } catch (Exception e) {
                    log.error("保存登录日志失败", e);
                }
            });
        } catch (Exception e) {
            log.error("提交日志保存任务失败", e);
        }
    }

    /**
     * 控制台打印登录日志
     */
    private void printLoginLog(LoginLogInfo logInfo) {
        if (logProperties.getPrintLoginLog() == null || !logProperties.getPrintLoginLog()) {
            return;
        }

        try {
            StringBuilder logMsg = new StringBuilder();
            logMsg.append("【登录日志】");
            logMsg.append("类型: ").append(logInfo.getLoginType());
            logMsg.append(", 用户ID: ").append(logInfo.getUserId());
            
            if (StrUtil.isNotBlank(logInfo.getUsername())) {
                logMsg.append(", 用户名: ").append(logInfo.getUsername());
            }
            
            logMsg.append(", 登录IP: ").append(logInfo.getLoginIp());
            
            if (StrUtil.isNotBlank(logInfo.getBrowser())) {
                logMsg.append(", 浏览器: ").append(logInfo.getBrowser());
            }
            
            if (StrUtil.isNotBlank(logInfo.getOs())) {
                logMsg.append(", 系统: ").append(logInfo.getOs());
            }
            
            logMsg.append(", 信息: ").append(logInfo.getLoginMessage());
            
            if (logInfo.getLoginStatus() == 1) {
                log.info(logMsg.toString());
            } else {
                log.warn(logMsg.toString());
            }
        } catch (Exception e) {
            log.debug("打印登录日志失败", e);
        }
    }
}
