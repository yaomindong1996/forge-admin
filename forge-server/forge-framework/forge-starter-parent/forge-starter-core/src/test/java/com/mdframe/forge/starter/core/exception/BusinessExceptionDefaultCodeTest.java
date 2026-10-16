package com.mdframe.forge.starter.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionDefaultCodeTest {

    @Test
    void messageOnlyShouldDefaultToClientError() {
        BusinessException exception = new BusinessException("任务名称不能为空");
        assertThat(exception.getCode()).isEqualTo(400);
        assertThat(exception.getMessage()).isEqualTo("任务名称不能为空");
    }

    @Test
    void explicitServerErrorCodeShouldBePreserved() {
        BusinessException exception = new BusinessException(500, "幂等执行命令不完整");
        assertThat(exception.getCode()).isEqualTo(500);
    }

    @Test
    void wrappedCauseShouldRemainServerErrorByDefault() {
        BusinessException exception = new BusinessException("落库失败", new IllegalStateException("x"));
        assertThat(exception.getCode()).isEqualTo(500);
    }
}
