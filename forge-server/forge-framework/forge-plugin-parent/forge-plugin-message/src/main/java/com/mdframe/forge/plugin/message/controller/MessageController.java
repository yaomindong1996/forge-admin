package com.mdframe.forge.plugin.message.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.message.domain.dto.MessageQueryDTO;
import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/message")
@ApiDecrypt
@ApiEncrypt
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 发送消息（需 message:send 权限；内部 Java 调用走 MessageService 不受此门禁影响）
     */
    @PostMapping("/send")
    @SaCheckPermission("message:send")
    public RespInfo<?> send(@RequestBody MessageSendRequestDTO req) {
        return RespInfo.success(messageService.send(req));
    }

    /**
     * 分页查询当前用户消息列表
     */
    @PostMapping("/page")
    @ApiPermissionIgnore
    public RespInfo<?> page(@RequestBody(required = false) MessageQueryDTO query,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        return RespInfo.success(messageService.pageUserMessages(SessionHelper.getUserId(), query, pageNum, pageSize));
    }

    /**
     * 分页查询当前用户消息列表（兼容无请求体查询）。
     */
    @GetMapping("/page")
    @ApiPermissionIgnore
    public RespInfo<?> pageByGet(MessageQueryDTO query,
                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return RespInfo.success(messageService.pageUserMessages(SessionHelper.getUserId(), query, pageNum, pageSize));
    }

    /**
     * 查询消息详情
     */
    @GetMapping("/{messageId}")
    @ApiPermissionIgnore
    public RespInfo<?> getDetail(@PathVariable Long messageId) {
        return RespInfo.success(messageService.getMessageDetail(messageId, SessionHelper.getUserId()));
    }

    /**
     * 标记单条消息为已读
     */
    @PostMapping("/{messageId}/read")
    @ApiPermissionIgnore
    public RespInfo<?> markRead(@PathVariable Long messageId) {
        messageService.markRead(messageId, SessionHelper.getUserId());
        return RespInfo.success();
    }

    /**
     * 批量标记为已读
     */
    @PostMapping("/read/batch")
    @ApiPermissionIgnore
    public RespInfo<?> markReadBatch(@RequestBody List<Long> messageIds) {
        messageService.markReadBatch(messageIds, SessionHelper.getUserId());
        return RespInfo.success();
    }

    /**
     * 全部标记为已读
     */
    @PostMapping("/read/all")
    @ApiPermissionIgnore
    public RespInfo<?> markAllRead() {
        messageService.markAllRead(SessionHelper.getUserId());
        return RespInfo.success();
    }

    /**
     * 查询未读消息统计
     */
    @GetMapping("/unread/count")
    @ApiPermissionIgnore
    public RespInfo<?> getUnreadCount() {
        return RespInfo.success(messageService.getUnreadCount(SessionHelper.getUserId()));
    }
}
