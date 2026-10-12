package com.mdframe.forge.starter.web.filter;

import java.io.IOException;

/** 不携带请求正文，避免异常日志暴露业务数据。 */
public final class RequestBodyTooLargeException extends IOException {

    public RequestBodyTooLargeException() {
        super("请求体超过大小限制");
    }
}
