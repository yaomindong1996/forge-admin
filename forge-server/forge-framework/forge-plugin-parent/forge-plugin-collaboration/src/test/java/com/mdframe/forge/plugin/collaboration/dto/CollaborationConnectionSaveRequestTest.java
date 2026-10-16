package com.mdframe.forge.plugin.collaboration.dto;

import com.mdframe.forge.starter.social.domain.entity.SysSocialConfig;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 新增连接只落连接根字段，不得把应用凭据写回 sys_social_config。
 */
class CollaborationConnectionSaveRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void toEntityLeavesLegacyCredentialsUnset() {
        CollaborationConnectionSaveRequest request = new CollaborationConnectionSaveRequest();
        request.setPlatform("WECHAT_ENTERPRISE");
        request.setPlatformName("企业微信");
        request.setConnectionName("XX科技企业微信");
        request.setEnterpriseId("ww-demo");
        request.setConnectionType("CORP_INTERNAL");
        request.setIdentityPolicy("BIND_ONLY");
        request.setStatus(1);

        SysSocialConfig entity = request.toEntity();

        assertThat(entity.getPlatform()).isEqualTo("WECHAT_ENTERPRISE");
        assertThat(entity.getConnectionName()).isEqualTo("XX科技企业微信");
        assertThat(entity.getClientId()).isNull();
        assertThat(entity.getClientSecret()).isNull();
        assertThat(entity.getAgentId()).isNull();
    }

    @Test
    void shouldRejectBlankPlatformAndConnectionName() {
        CollaborationConnectionSaveRequest request = new CollaborationConnectionSaveRequest();

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("platform", "connectionName");
    }
}
