package com.mdframe.forge.starter.datascope.context;

/**
 * 数据权限上下文持有者
 * 用于控制是否启用数据权限过滤
 */
public class DataScopeContextHolder {
    
    private static final ThreadLocal<Boolean> SKIP_DATA_SCOPE = new ThreadLocal<>();
    
    /**
     * 设置跳过数据权限控制
     */
    public static void skipDataScope() {
        SKIP_DATA_SCOPE.set(true);
    }
    
    /**
     * 清除跳过标记
     */
    public static void clearSkip() {
        SKIP_DATA_SCOPE.remove();
    }
    
    /**
     * 判断是否跳过数据权限控制
     */
    public static boolean isSkip() {
        Boolean skip = SKIP_DATA_SCOPE.get();
        return skip != null && skip;
    }
    
    /**
     * 执行无数据权限控制的操作
     * 
     * @param runnable 要执行的操作
     */
    public static void executeWithoutDataScope(Runnable runnable) {
        try {
            skipDataScope();
            runnable.run();
        } finally {
            clearSkip();
        }
    }
    
    /**
     * 执行无数据权限控制的操作并返回结果
     * 
     * @param supplier 要执行的操作
     * @return 执行结果
     */
    public static <T> T executeWithoutDataScope(java.util.function.Supplier<T> supplier) {
        try {
            skipDataScope();
            return supplier.get();
        } finally {
            clearSkip();
        }
    }
}
