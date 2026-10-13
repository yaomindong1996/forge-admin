package com.mdframe.forge.plugin.message.service.impl;

import com.mdframe.forge.plugin.message.domain.entity.SysMessageBizType;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageBizTypeAccessPolicyTest {

    @Test
    void builtInBizTypesShouldBeProtected() {
        assertThat(MessageBizTypeAccessPolicy.isBuiltIn("FLOW_TODO")).isTrue();
        assertThat(MessageBizTypeAccessPolicy.isBuiltIn("flow_task_overdue")).isTrue();
        assertThat(MessageBizTypeAccessPolicy.isBuiltIn("ORDER")).isFalse();
    }

    @Test
    void deleteShouldRejectBuiltIn() {
        SysMessageBizType entity = new SysMessageBizType();
        entity.setBizType("FLOW_TODO");
        assertThatThrownBy(() -> MessageBizTypeAccessPolicy.assertDeletable(entity))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能删除");
    }

    @Test
    void customBizTypeCanDelete() {
        SysMessageBizType entity = new SysMessageBizType();
        entity.setBizType("PURCHASE_ORDER");
        assertThatCode(() -> MessageBizTypeAccessPolicy.assertDeletable(entity)).doesNotThrowAnyException();
    }
}
