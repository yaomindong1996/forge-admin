package com.mdframe.forge.plugin.message.controller;

import com.mdframe.forge.plugin.message.domain.dto.MessageManageQueryDTO;
import com.mdframe.forge.plugin.message.service.MessageManageService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/message/manage")
public class MessageManageController {

    private final MessageManageService messageManageService;

    public MessageManageController(MessageManageService messageManageService) {
        this.messageManageService = messageManageService;
    }

    @PostMapping("/page")
    public RespInfo<?> page(
            @RequestBody MessageManageQueryDTO query,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return RespInfo.success(messageManageService.pageMessages(query, pageNum, pageSize));
    }

    @GetMapping("/{messageId}/detail")
    public RespInfo<?> getDetail(@PathVariable Long messageId) {
        return RespInfo.success(messageManageService.getMessageDetail(messageId));
    }
}