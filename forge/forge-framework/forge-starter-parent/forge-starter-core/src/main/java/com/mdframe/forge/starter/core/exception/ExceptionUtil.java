package com.mdframe.forge.starter.core.exception;

/**
 * 异常工具类
 * 提供便捷的异常抛出方法
 */
public class ExceptionUtil {

    private ExceptionUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 抛出业务异常
     *
     * @param message 错误消息
     */
    public static void throwBiz(String message) {
        throw new BusinessException(message);
    }

    /**
     * 抛出业务异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public static void throwBiz(Integer code, String message) {
        throw new BusinessException(code, message);
    }

    /**
     * 抛出业务异常
     *
     * @param code    错误码
     * @param message 错误消息
     * @param data    详细错误信息
     */
    public static void throwBiz(Integer code, String message, Object data) {
        throw new BusinessException(code, message, data);
    }

    /**
     * 条件判断，如果为true则抛出异常
     *
     * @param condition 条件表达式
     * @param message   错误消息
     */
    public static void throwIf(boolean condition, String message) {
        if (condition) {
            throw new BusinessException(message);
        }
    }

    /**
     * 条件判断，如果为true则抛出异常
     *
     * @param condition 条件表达式
     * @param code      错误码
     * @param message   错误消息
     */
    public static void throwIf(boolean condition, Integer code, String message) {
        if (condition) {
            throw new BusinessException(code, message);
        }
    }

    /**
     * 条件判断，如果为false则抛出异常
     *
     * @param condition 条件表达式
     * @param message   错误消息
     */
    public static void throwIfNot(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }

    /**
     * 条件判断，如果为false则抛出异常
     *
     * @param condition 条件表达式
     * @param code      错误码
     * @param message   错误消息
     */
    public static void throwIfNot(boolean condition, Integer code, String message) {
        if (!condition) {
            throw new BusinessException(code, message);
        }
    }

    /**
     * 判断对象为空则抛出异常
     *
     * @param object  待检查对象
     * @param message 错误消息
     */
    public static void throwIfNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(message);
        }
    }

    /**
     * 判断对象为空则抛出异常
     *
     * @param object  待检查对象
     * @param code    错误码
     * @param message 错误消息
     */
    public static void throwIfNull(Object object, Integer code, String message) {
        if (object == null) {
            throw new BusinessException(code, message);
        }
    }

    /**
     * 判断字符串为空则抛出异常
     *
     * @param str     待检查字符串
     * @param message 错误消息
     */
    public static void throwIfBlank(String str, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new BusinessException(message);
        }
    }

    /**
     * 判断字符串为空则抛出异常
     *
     * @param str     待检查字符串
     * @param code    错误码
     * @param message 错误消息
     */
    public static void throwIfBlank(String str, Integer code, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new BusinessException(code, message);
        }
    }

    /**
     * 条件判断，返回true则抛出异常，否则返回对象
     *
     * @param condition 条件表达式
     * @param message   错误消息
     * @param object    返回对象
     * @param <T>       对象类型
     * @return 对象
     */
    public static <T> T throwIfOrElse(boolean condition, String message, T object) {
        if (condition) {
            throw new BusinessException(message);
        }
        return object;
    }

    /**
     * 包装检查型异常为运行时异常
     *
     * @param throwable 异常
     * @return 业务异常
     */
    public static BusinessException wrap(Throwable throwable) {
        if (throwable instanceof BusinessException) {
            return (BusinessException) throwable;
        }
        return new BusinessException(throwable.getMessage(), throwable);
    }

    /**
     * 包装检查型异常为运行时异常
     *
     * @param throwable 异常
     * @param message   自定义错误消息
     * @return 业务异常
     */
    public static BusinessException wrap(Throwable throwable, String message) {
        return new BusinessException(message, throwable);
    }

    /**
     * 包装检查型异常为运行时异常
     *
     * @param throwable 异常
     * @param code      错误码
     * @param message   自定义错误消息
     * @return 业务异常
     */
    public static BusinessException wrap(Throwable throwable, Integer code, String message) {
        return new BusinessException(code, message, throwable);
    }
}
