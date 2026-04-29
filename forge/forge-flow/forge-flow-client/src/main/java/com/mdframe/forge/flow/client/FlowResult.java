package com.mdframe.forge.flow.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 流程服务返回结果
 * 
 * @param <T> 数据类型
 * @author forge
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowResult<T> {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String msg;

    /**
     * 数据
     */
    private T data;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return code != null && code == 200;
    }

    /**
     * 创建成功结果
     */
    public static <T> FlowResult<T> success(T data) {
        FlowResult<T> result = new FlowResult<>();
        result.setCode(200);
        result.setMsg("成功");
        result.setData(data);
        return result;
    }

    /**
     * 创建失败结果
     */
    public static <T> FlowResult<T> error(String msg) {
        FlowResult<T> result = new FlowResult<>();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }
}