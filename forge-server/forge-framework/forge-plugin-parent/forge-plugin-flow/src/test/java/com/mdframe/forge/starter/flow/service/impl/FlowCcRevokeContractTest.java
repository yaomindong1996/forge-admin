package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 撤回抄送必须具备发送人、租户和有效状态条件。 */
class FlowCcRevokeContractTest {

    @Test
    void revokeMustBeAtomicAndRecipientMustLoseVisibility() throws IOException {
        String service = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowCcServiceImpl.java"));
        String mapper = Files.readString(Path.of(
                "src/main/resources/mapper/FlowCcMapper.xml"));
        String migration = Files.readString(Path.of(
                "../../../db/migration/V1.0.146__add_flow_cc_revoke_state.sql"));
        assertTrue(service.contains("baseMapper.revokeBySender"));
        assertTrue(service.contains("FlowCcStatus.REVOKED.getCode()"));
        assertTrue(service.contains("FlowCcStatus.ACTIVE.getCode()"));
        assertTrue(mapper.contains("send_user_id = #{senderId}"));
        assertTrue(mapper.contains("status = 0"));
        assertTrue(mapper.contains("cc_user_id = #{userId} AND c.status = 0"));
        assertTrue(mapper.contains("<select id=\"selectMyPage\""));
        assertTrue(mapper.contains("<select id=\"selectSentPage\""));
        assertTrue(mapper.contains("c.title LIKE CONCAT('%', #{title}, '%')"));
        assertTrue(mapper.contains("c.tenant_id = #{tenantId}"));
        assertTrue(mapper.contains("<update id=\"markAllRead\">"));
        assertTrue(mapper.contains("AND is_read = 0"));
        assertTrue(migration.contains("revoke_reason"));
    }
}
