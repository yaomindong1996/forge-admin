package com.mdframe.forge.starter.core.context;

import java.util.Optional;

/**
 * 显式执行身份上下文。Scope 关闭时恢复外层身份，禁止清理 Sa-Token Session。
 */
public final class ExecutionIdentityContextHolder {

    private static final ThreadLocal<ExecutionIdentity> HOLDER = new ThreadLocal<>();

    private ExecutionIdentityContextHolder() {
    }

    public static Optional<ExecutionIdentity> current() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static Scope open(ExecutionIdentity identity) {
        if (identity == null) {
            throw new IllegalArgumentException("identity 不能为空");
        }
        ExecutionIdentity previous = HOLDER.get();
        HOLDER.set(identity);
        return new Scope(previous);
    }

    public static Scope suspend() {
        ExecutionIdentity previous = HOLDER.get();
        HOLDER.remove();
        return new Scope(previous);
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static final class Scope implements AutoCloseable {

        private final ExecutionIdentity previous;
        private boolean closed;

        private Scope(ExecutionIdentity previous) {
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
