package com.mdframe.forge.starter.trans.handler;

import com.mdframe.forge.starter.trans.model.TransContext;

/**
 * 翻译处理器接口
 * 所有翻译实现需实现该接口
 */
public interface TransHandler {

    /**
     * 是否支持当前上下文
     */
    default boolean supports(TransContext context) {
        return true;
    }

    /**
     * 执行翻译
     *
     * @param context 翻译上下文（包含原值、注解信息等）
     * @return 翻译后的值（通常为String）
     */
    Object translate(TransContext context);
}
