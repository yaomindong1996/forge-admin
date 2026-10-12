package com.mdframe.forge.starter.core.context;

import com.mdframe.forge.starter.core.session.LoginUser;

/** 后台任务的受信用户 Scope。只能由服务端认证链创建，不替代 Sa-Token 登录或机器客户端校验。 */
public final class LoginUserContextHolder {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginUserContextHolder() {
    }

    public static LoginUser current() {
        LoginUser user = HOLDER.get();
        return user == null ? null : LoginUserSnapshot.copy(user);
    }

    public static Scope open(LoginUser user) {
        if (user == null || user.getUserId() == null || user.getUserId() <= 0
                || user.getTenantId() == null || user.getTenantId() <= 0) {
            throw new IllegalArgumentException("任务执行用户和租户必须经过验证");
        }
        LoginUser previous = HOLDER.get();
        HOLDER.set(LoginUserSnapshot.copy(user));
        return new Scope(previous);
    }

    public static final class Scope implements AutoCloseable {
        private final LoginUser previous;
        private boolean closed;

        private Scope(LoginUser previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previous == null) {
                HOLDER.remove();
            } else {
                HOLDER.set(previous);
            }
        }
    }
}
