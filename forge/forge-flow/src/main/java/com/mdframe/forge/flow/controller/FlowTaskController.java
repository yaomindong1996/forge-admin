package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 流程任务接口（我的待办/已办/我发起的）
 */
@RestController
@RequestMapping("/api/flow/task")
@RequiredArgsConstructor
public class FlowTaskController {

    private final FlowTaskService flowTaskService;

    /**
     * 我的待办任务
     */
    @GetMapping("/todo")
    public RespInfo<IPage<FlowTask>> todo(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam String userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {
        
        Page<FlowTask> page = new Page<>(pageNum, pageSize);
        IPage<FlowTask> result = flowTaskService.todoTasks(page, userId, title, category);
        return RespInfo.success(result);
    }

    /**
     * 我的已办任务
     */
    @GetMapping("/done")
    public RespInfo<IPage<FlowTask>> done(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam String userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {
        
        Page<FlowTask> page = new Page<>(pageNum, pageSize);
        IPage<FlowTask> result = flowTaskService.doneTasks(page, userId, title, category);
        return RespInfo.success(result);
    }

    /**
     * 我发起的流程
     */
    @GetMapping("/started")
    public RespInfo<IPage<FlowTask>> started(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam String userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {
        
        Page<FlowTask> page = new Page<>(pageNum, pageSize);
        IPage<FlowTask> result = flowTaskService.startedTasks(page, userId, title, category);
        return RespInfo.success(result);
    }

    /**
     * 候选任务（未签收的任务）
     */
    @GetMapping("/candidate")
    public RespInfo<IPage<FlowTask>> candidateTasks(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam String userId,
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String title) {
        
        Page<FlowTask> page = new Page<>(pageNum, pageSize);
        IPage<FlowTask> result = flowTaskService.candidateTasks(page, userId, groupId, title);
        return RespInfo.success(result);
    }

    /**
     * 签收任务
     */
    @PostMapping("/claim")
    public RespInfo<Void> claim(@RequestParam String taskId, @RequestParam String userId) {
        flowTaskService.claimTask(taskId, userId);
        return RespInfo.success("签收成功", null);
    }

    /**
     * 审批通过
     */
    @PostMapping("/approve")
    public RespInfo<Void> approve(@RequestBody Map<String, Object> params) {
        String taskId = (String) params.get("taskId");
        String userId = (String) params.get("userId");
        String comment = (String) params.get("comment");
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) params.get("variables");
        
        flowTaskService.approve(taskId, userId, comment, variables);
        return RespInfo.success("审批通过", null);
    }

    /**
     * 审批驳回
     */
    @PostMapping("/reject")
    public RespInfo<Void> reject(@RequestBody Map<String, Object> params) {
        String taskId = (String) params.get("taskId");
        String userId = (String) params.get("userId");
        String comment = (String) params.get("comment");
        
        flowTaskService.reject(taskId, userId, comment);
        return RespInfo.success("已驳回", null);
    }

    /**
     * 转办
     */
    @PostMapping("/delegate")
    public RespInfo<Void> delegate(@RequestBody Map<String, Object> params) {
        String taskId = (String) params.get("taskId");
        String userId = (String) params.get("userId");
        String targetUserId = (String) params.get("targetUserId");
        String comment = (String) params.get("comment");
        
        flowTaskService.delegate(taskId, userId, targetUserId, comment);
        return RespInfo.success("转办成功", null);
    }

    /**
     * 撤回流程
     */
    @PostMapping("/withdraw")
    public RespInfo<Void> withdraw(@RequestBody Map<String, Object> params) {
        String processInstanceId = (String) params.get("processInstanceId");
        String userId = (String) params.get("userId");
        
        flowTaskService.withdraw(processInstanceId, userId);
        return RespInfo.success("撤回成功", null);
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{taskId}")
    public RespInfo<FlowTask> getById(@PathVariable String taskId) {
        FlowTask task = flowTaskService.getTaskDetail(taskId);
        return RespInfo.success(task);
    }

    /**
     * 获取流程图（高亮当前节点）
     */
    @GetMapping("/diagram/{processInstanceId}")
    public ResponseEntity<byte[]> getProcessDiagram(@PathVariable String processInstanceId) {
        byte[] diagram = flowTaskService.getProcessDiagram(processInstanceId);
        
        if (diagram == null || diagram.length == 0) {
            return ResponseEntity.notFound().build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(diagram);
    }

    /**
     * 催办
     */
    @PostMapping("/remind")
    public RespInfo<Void> remind(@RequestParam String taskId) {
        flowTaskService.remind(taskId);
        return RespInfo.success("催办成功", null);
    }
}