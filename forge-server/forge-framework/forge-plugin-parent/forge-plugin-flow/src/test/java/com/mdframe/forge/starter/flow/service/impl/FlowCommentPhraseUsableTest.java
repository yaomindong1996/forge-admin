package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.mapper.FlowCommentPhraseMapper;
import com.mdframe.forge.starter.flow.vo.FlowCommentPhraseVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@DisplayName("FlowCommentPhraseServiceImpl.listUsable")
class FlowCommentPhraseUsableTest {

    @Test
    @DisplayName("personal and tenant phrases with the same content are listed once, personal first")
    void usablePhrasesAreDistinctByContent() {
        FlowCommentPhraseMapper mapper = mock(FlowCommentPhraseMapper.class);
        FlowCommentPhraseServiceImpl service = new FlowCommentPhraseServiceImpl(mapper);
        when(mapper.selectUsable(any(), any(), any())).thenReturn(List.of(
                phrase(8L, "同意"), phrase(7L, "批阅"), phrase(1L, " 同意 "), phrase(2L, "已阅"), phrase(9L, " ")));
        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getTenantId).thenReturn(1L);
            session.when(SessionHelper::getUserId).thenReturn(1L);

            List<FlowCommentPhraseVO> usable = service.listUsable("APPROVE");

            assertThat(usable).extracting(FlowCommentPhraseVO::getId).containsExactly(8L, 7L, 2L);
        }
    }

    private FlowCommentPhraseVO phrase(Long id, String content) {
        FlowCommentPhraseVO vo = new FlowCommentPhraseVO();
        vo.setId(id);
        vo.setContent(content);
        return vo;
    }
}
