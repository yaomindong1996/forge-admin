package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.entity.FlowCc;
import com.mdframe.forge.starter.flow.service.FlowCcService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程抄送管理接口
 */
@RestController
@RequestMapping("/api/flow/cc")
@RequiredArgsConstructor
public class FlowCcController {

    private final FlowCcService flowCcService;

    /**
     * 发送抄送
     */
    @PostMapping("/send")
    public RespInfo<Void> sendCc(@RequestBody Map<String, Object> params) {
        String processInstanceId = (String) params.get("processInstanceId");
        String processDefKey = (String) params.get("processDefKey");
        String taskId = (String) params.get("taskId");
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        String businessKey = (String) params.get("businessKey");
        @SuppressWarnings("unchecked")
        List<String> ccUserIds = (List<String>) params.get("ccUserIds");
        @SuppressWarnings("unchecked")
        List<String> ccUserNames = (List<String>) params.get("ccUserNames");
        String sendUserId = (String) params.get("sendUserId");
        String sendUserName = (String) params.get("sendUserName");
        
        flowCcService.sendCc(processInstanceId, processDefKey, taskId, title, content,
                businessKey, ccUserIds, ccUserNames, sendUserId, sendUserName);
        
        return RespInfo.success("抄送成功", null);
    }

    /**
     * 我的抄送（抄送给我的）
     */
    @GetMapping("/my")
    public RespInfo<IPage<FlowCc>> myCc(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam String userId,
            @RequestParam(required = false) Integer isRead) {
        
        Page<FlowCc> page = new Page<>(pageNum, pageSize);
        IPage<FlowCc> result = flowCcService.myCc(page, userId, isRead);
        return RespInfo.success(result);
    }

    /**
     * 我发送的抄送
     */
    @GetMapping("/sent")
    public RespInfo<IPage<FlowCc>> sentCc(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam String userId) {
        
        Page<FlowCc> page = new Page<>(pageNum, pageSize);
        IPage<FlowCc> result = flowCcService.sentCc(page, userId);
        return RespInfo.success(result);
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

    /**
     * 获取未读抄送数量
     */
    @GetMapping("/unread/count")
    public RespInfo<Map<String, Object>> countUnread(@RequestParam String userId) {
        long count = flowCcService.countUnread(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return RespInfo.success(result);
    }
}