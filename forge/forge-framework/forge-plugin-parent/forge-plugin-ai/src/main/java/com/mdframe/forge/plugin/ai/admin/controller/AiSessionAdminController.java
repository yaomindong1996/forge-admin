package com.mdframe.forge.plugin.ai.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.session.dto.AiSessionPageQuery;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.plugin.ai.session.vo.AiSessionStatisticsVO;
import com.mdframe.forge.plugin.ai.session.vo.AiSessionVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ai/admin/session")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class AiSessionAdminController {

    private final AiChatSessionService sessionService;
    private final AiChatRecordService recordService;

    @GetMapping("/page")
    public RespInfo<Page<AiSessionVO>> page(AiSessionPageQuery query) {
        return RespInfo.success(sessionService.adminPage(query));
    }

    @GetMapping("/{sessionId}/messages")
    public RespInfo<List<AiChatRecord>> messages(@PathVariable String sessionId) {
        return RespInfo.success(recordService.listBySession(sessionId));
    }

    @DeleteMapping("/{sessionId}")
    public RespInfo<Void> delete(@PathVariable String sessionId) {
        sessionService.deleteSession(sessionId);
        return RespInfo.success();
    }

    @GetMapping("/statistics")
    public RespInfo<AiSessionStatisticsVO> statistics() {
        return RespInfo.success(sessionService.getStatistics());
    }
}
