package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static contract checks for the tenant and participant boundaries of copy
 * (CC) records. These checks deliberately inspect the service source so the
 * security predicates remain visible during refactors.
 */
class FlowCcSecurityContractTest {

    @Test
    void ccQueriesAndMutationsMustBeTenantAndParticipantScoped() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowCcServiceImpl.java"));

        int myCcStart = source.indexOf("public IPage<FlowCc> myCc");
        int sentCcStart = source.indexOf("public IPage<FlowCc> sentCc");
        int markReadStart = source.indexOf("public void markRead");
        int batchReadStart = source.indexOf("public void batchMarkRead");
        int countStart = source.indexOf("public long countUnread");
        int visibleStart = source.indexOf("public FlowCc getVisibleById");
        assertTrue(myCcStart >= 0 && sentCcStart > myCcStart && markReadStart > sentCcStart);
        assertTrue(batchReadStart > markReadStart && countStart > batchReadStart && visibleStart > countStart);

        String myCc = source.substring(myCcStart, sentCcStart);
        String sentCc = source.substring(sentCcStart, markReadStart);
        String markRead = source.substring(markReadStart, batchReadStart);
        String batchRead = source.substring(batchReadStart, countStart);
        String countUnread = source.substring(countStart, visibleStart);
        String visible = source.substring(visibleStart);

        assertTrue(myCc.contains("baseMapper.selectMyPage"));
        assertTrue(myCc.contains("normalizeSearchText(title)"));
        assertTrue(sentCc.contains("baseMapper.selectSentPage"));
        assertTrue(sentCc.contains("normalizeSearchText(title)"));
        assertTrue(markRead.contains("FlowCc::getTenantId"));
        assertTrue(markRead.contains("FlowCc::getCcUserId"));
        assertTrue(markRead.contains("FlowCc::getStatus"));
        assertTrue(markRead.contains("FLOW_CC_NOT_VISIBLE"));
        assertTrue(batchRead.contains("FlowCc::getTenantId"));
        assertTrue(batchRead.contains("FlowCc::getCcUserId"));
        assertTrue(batchRead.contains("FlowCc::getStatus"));
        assertTrue(source.contains("public int markAllRead()"));
        assertTrue(source.contains("baseMapper.markAllRead"));
        assertTrue(countUnread.contains("baseMapper.countWorkspaceUnread"));
        assertTrue(countUnread.contains("requireTenant()"));
        assertTrue(visible.contains("FlowCc::getTenantId"));
        assertTrue(visible.contains("FlowCc::getCcUserId"));
        assertTrue(visible.contains("FlowCc::getSendUserId"));
    }
}
