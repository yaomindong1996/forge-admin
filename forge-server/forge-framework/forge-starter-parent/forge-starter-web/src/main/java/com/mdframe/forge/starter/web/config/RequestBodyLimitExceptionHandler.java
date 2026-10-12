package com.mdframe.forge.starter.web.config;

import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.web.filter.RequestBodyTooLargeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 在通用异常处理器之前识别 Jackson 包装的限流异常，避免超限被错误返回为 400/500。 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestBodyLimitExceptionHandler {

    @ExceptionHandler(RequestBodyTooLargeException.class)
    public ResponseEntity<RespInfo<Void>> handle() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(RespInfo.error(HttpStatus.PAYLOAD_TOO_LARGE.value(), "请求体超过大小限制"));
    }
}
