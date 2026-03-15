package com.mdframe.forge.flow.controller;

import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.entity.FlowComment;
import com.mdframe.forge.starter.flow.service.FlowCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 流程审批意见接口
 */
@RestController
@RequestMapping("/api/flow/comment")
@RequiredArgsConstructor
public class FlowCommentController {

    private final FlowCommentService flowCommentService;

    /**
     * 添加审批意见
     */
    @PostMapping
    public RespInfo<FlowComment> addComment(@RequestBody Map<String, Object> params) {
        String processInstanceId = (String) params.get("processInstanceId");
        String processDefKey = (String) params.get("processDefKey");
        String taskId = (String) params.get("taskId");
        String taskName = (String) params.get("taskName");
        String type = (String) params.get("type");
        String message = (String) params.get("message");
        String userId = (String) params.get("userId");
        String userName = (String) params.get("userName");
        
        FlowComment comment = flowCommentService.addComment(processInstanceId, processDefKey,
                taskId, taskName, type, message, userId, userName);
        
        return RespInfo.success("添加成功", comment);
    }

    /**
     * 获取流程的所有审批意见（审批历史）
     */
    @GetMapping("/process/{processInstanceId}")
    public RespInfo<List<FlowComment>> getByProcessInstanceId(@PathVariable String processInstanceId) {
        List<FlowComment> comments = flowCommentService.getCommentsByProcessInstanceId(processInstanceId);
        return RespInfo.success(comments);
    }

    /**
     * 获取任务的审批意见
     */
    @GetMapping("/task/{taskId}")
    public RespInfo<List<FlowComment>> getByTaskId(@PathVariable String taskId) {
        List<FlowComment> comments = flowCommentService.getCommentsByTaskId(taskId);
        return RespInfo.success(comments);
    }

    /**
     * 添加流程事件
     */
    @PostMapping("/event")
    public RespInfo<FlowComment> addEvent(@RequestBody Map<String, Object> params) {
        String processInstanceId = (String) params.get("processInstanceId");
        String processDefKey = (String) params.get("processDefKey");
        String message = (String) params.get("message");
        String userId = (String) params.get("userId");
        String userName = (String) params.get("userName");
        
        FlowComment event = flowCommentService.addEvent(processInstanceId, processDefKey,
                message, userId, userName);
        
        return RespInfo.success("添加成功", event);
    }
}