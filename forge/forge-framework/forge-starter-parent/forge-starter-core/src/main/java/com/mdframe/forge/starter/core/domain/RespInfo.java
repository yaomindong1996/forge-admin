package com.mdframe.forge.starter.core.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应结果封装类
 *
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RespInfo<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    public RespInfo(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应(无数据)
     */
    public static <T> RespInfo<T> success() {
        return new RespInfo<>(200, "操作成功", null);
    }

    /**
     * 成功响应(带数据)
     */
    public static <T> RespInfo<T> success(T data) {
        return new RespInfo<>(200, "操作成功", data);
    }

    /**
     * 成功响应(自定义消息)
     */
    public static <T> RespInfo<T> success(String message, T data) {
        return new RespInfo<>(200, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> RespInfo<T> error() {
        return new RespInfo<>(500, "操作失败", null);
    }

    /**
     * 失败响应(自定义消息)
     */
    public static <T> RespInfo<T> error(String message) {
        return new RespInfo<>(500, message, null);
    }

    /**
     * 失败响应(自定义状态码和消息)
     */
    public static <T> RespInfo<T> error(Integer code, String message) {
        return new RespInfo<>(code, message, null);
    }

    /**
     * 自定义响应
     */
    public static <T> RespInfo<T> build(Integer code, String message, T data) {
        return new RespInfo<>(code, message, data);
    }
}
