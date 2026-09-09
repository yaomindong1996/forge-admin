package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.flow.dto.FlowCcSendDTO;
import com.mdframe.forge.flow.dto.FlowCcRevokeDTO;
import com.mdframe.forge.flow.identity.FlowSessionIdentity;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowCc;
import com.mdframe.forge.starter.flow.service.FlowCcService;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import com.mdframe.forge.starter.flow.vo.FlowCcUnreadCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程抄送管理接口
 */
@RestController
@RequestMapping("/api/flow/cc")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowCcController {

    private final FlowCcService flowCcService;
    private final FlowTaskService flowTaskService;

    /**
     * 发送抄送
     */
    @PostMapping("/send")
    public RespInfo<Void> sendCc(@RequestBody FlowCcSendDTO dto) {
        String sendUserId = FlowSessionIdentity.requireUserId(dto.getSendUserId());
        flowCcService.sendCcByCurrentUser(
                dto.getProcessInstanceId(), dto.getProcessDefKey(), dto.getTaskId(),
                dto.getTitle(), dto.getContent(), dto.getBusinessKey(),
                dto.getCcUserIds(), dto.getCcUserNames(),
                sendUserId, dto.getSendUserName());
        return RespInfo.success("抄送成功", null);
    }

    /**
     * 我的抄送（抄送给我的）
     */
    @GetMapping("/my")
    public RespInfo<IPage<FlowCc>> myCc(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Integer isRead,
            @RequestParam(required = false) String title) {
        String trustedUserId = FlowSessionIdentity.requireUserId(userId);
        Page<FlowCc> page = FlowSessionIdentity.page(pageNum, pageSize);
        IPage<FlowCc> result = flowCcService.myCc(page, trustedUserId, isRead, title);
        return RespInfo.success(result);
    }

    /**
     * 我发送的抄送
     */
    @GetMapping("/sent")
    public RespInfo<IPage<FlowCc>> sentCc(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String title) {
        String trustedUserId = FlowSessionIdentity.requireUserId(userId);
        Page<FlowCc> page = FlowSessionIdentity.page(pageNum, pageSize);
        IPage<FlowCc> result = flowCcService.sentCc(page, trustedUserId, title);
        return RespInfo.success(result);
    }

    /**
     * 获取抄送关联的业务表单信息
     */
    @GetMapping("/form/{id}")
    public RespInfo<TaskFormInfo> getCcFormInfo(@PathVariable String id) {
        String userId = FlowSessionIdentity.requireUserId(null);
        FlowCc cc = flowCcService.getVisibleById(id, userId);
        if (cc == null) {
            return RespInfo.error("抄送记录不存在");
        }
        TaskFormInfo formInfo = flowTaskService.getProcessFormInfo(
                cc.getProcessInstanceId(),
                cc.getBusinessKey(),
                cc.getProcessDefKey(),
                cc.getTaskId(),
                null);
        return RespInfo.success(formInfo);
    }

    /**
     * 标记已读
     */
    @PostMapping("/read/{id}")
    public RespInfo<Void> markRead(@PathVariable String id) {
        flowCcService.markRead(id);
        return RespInfo.success("已标记已读", null);
    }

    /**
     * 批量标记已读
     */
    @PostMapping("/read/batch")
    public RespInfo<Void> batchMarkRead(@RequestBody List<String> ids) {
        flowCcService.batchMarkRead(ids);
        return RespInfo.success("已批量标记已读", null);
    }

    /** 将当前接收人的全部有效未读抄送标记为已读。 */
    @PostMapping("/read/all")
    public RespInfo<Integer> markAllRead() {
        return RespInfo.success("已全部标记已读", flowCcService.markAllRead());
    }

    /** 发送人撤回临时抄送。 */
    @PostMapping("/revoke/{id}")
    public RespInfo<Void> revoke(@PathVariable String id, @RequestBody(required = false) FlowCcRevokeDTO dto) {
        flowCcService.revoke(id, dto == null ? null : dto.getReason());
        return RespInfo.success("抄送已撤回", null);
    }

    /**
     * 获取未读抄送数量
     */
    @GetMapping("/unread/count")
    public RespInfo<FlowCcUnreadCountVO> countUnread(@RequestParam(required = false) String userId) {
        long count = flowCcService.countUnread(FlowSessionIdentity.requireUserId(userId));
        return RespInfo.success(new FlowCcUnreadCountVO(count));
    }
}
