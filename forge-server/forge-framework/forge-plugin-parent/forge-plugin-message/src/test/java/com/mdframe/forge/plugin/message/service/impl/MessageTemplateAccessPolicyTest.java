package com.mdframe.forge.plugin.message.service.impl;

import com.mdframe.forge.plugin.message.domain.entity.SysMessageTemplate;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageTemplateAccessPolicyTest {

    @Test
    void builtInCodesShouldBeProtected() {
        assertThat(MessageTemplateAccessPolicy.isBuiltIn("FLOW_TODO_CARD")).isTrue();
        assertThat(MessageTemplateAccessPolicy.isBuiltIn("FLOW_CUSTOM_X")).isTrue();
        assertThat(MessageTemplateAccessPolicy.isBuiltIn("CUSTOM_NOTICE")).isFalse();
    }

    @Test
    void deleteShouldRejectBuiltIn() {
        SysMessageTemplate template = new SysMessageTemplate();
        template.setTemplateCode("SYSTEM_NOTICE");
        assertThatThrownBy(() -> MessageTemplateAccessPolicy.assertDeletable(template))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能删除");
    }

    @Test
    void customTemplateCanDeleteAndRename() {
        SysMessageTemplate template = new SysMessageTemplate();
        template.setTemplateCode("MY_NOTICE");
        assertThatCode(() -> MessageTemplateAccessPolicy.assertDeletable(template)).doesNotThrowAnyException();
        assertThatCode(() -> MessageTemplateAccessPolicy.assertMutable(template, "MY_NOTICE_V2"))
                .doesNotThrowAnyException();
    }
}
