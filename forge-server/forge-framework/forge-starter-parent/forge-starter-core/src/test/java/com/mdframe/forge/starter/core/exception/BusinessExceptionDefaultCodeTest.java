package com.mdframe.forge.starter.core.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessExceptionDefaultCodeTest {

    @Test
    void messageOnlyShouldDefaultToClientError() {
        BusinessException exception = new BusinessException("任务名称不能为空");
        assertEquals(400, exception.getCode());
        assertEquals("任务名称不能为空", exception.getMessage());
    }

    @Test
    void explicitServerErrorCodeShouldBePreserved() {
        BusinessException exception = new BusinessException(500, "幂等执行命令不完整");
        assertEquals(500, exception.getCode());
    }

    @Test
    void wrappedCauseShouldRemainServerErrorByDefault() {
        BusinessException exception = new BusinessException("落库失败", new IllegalStateException("x"));
        assertEquals(500, exception.getCode());
    }
}
