package com.mdframe.forge.starter.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    public Map<String, Object> todo(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam String userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {
        
        Page<FlowTask> page = new Page<>(pageNum, pageSize);
        IPage<FlowTask> result = flowTaskService.todoTasks(page, userId, title, category);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        return response;
    }

    /**
     * 我的已办任务
     */
    @GetMapping("/done")
    public Map<String, Object> done(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam String userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {
        
        Page<FlowTask> page = new Page<>(pageNum, pageSize);
        IPage<FlowTask> result = flowTaskService.doneTasks(page, userId, title, category);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        return response;
    }

    /**
     * 我发起的流程
     */
    @GetMapping("/started")
    public Map<String, Object> started(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam String userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {
        
        Page<FlowTask> page = new Page<>(pageNum, pageSize);
        IPage<FlowTask> result = flowTaskService.startedTasks(page, userId, title, category);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        return response;
    }

    /**
     * 签收任务
     */
    @PostMapping("/claim")
    public Map<String, Object> claim(@RequestParam String taskId, @RequestParam String userId) {
        flowTaskService.claimTask(taskId, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "签收成功");
        return response;
    }

    /**
     * 审批通过
     */
    @PostMapping("/approve")
    public Map<String, Object> approve(@RequestBody Map<String, Object> params) {
        String taskId = (String) params.get("taskId");
        String userId = (String) params.get("userId");
        String comment = (String) params.get("comment");
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) params.get("variables");
        
        flowTaskService.approve(taskId, userId, comment, variables);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "审批通过");
        return response;
    }

    /**
     * 审批驳回
     */
    @PostMapping("/reject")
    public Map<String, Object> reject(@RequestBody Map<String, Object> params) {
        String taskId = (String) params.get("taskId");
        String userId = (String) params.get("userId");
        String comment = (String) params.get("comment");
        
        flowTaskService.reject(taskId, userId, comment);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "已驳回");
        return response;
    }

    /**
     * 转办
     */
    @PostMapping("/delegate")
    public Map<String, Object> delegate(@RequestBody Map<String, Object> params) {
        String taskId = (String) params.get("taskId");
        String userId = (String) params.get("userId");
        String targetUserId = (String) params.get("targetUserId");
        String comment = (String) params.get("comment");
        
        flowTaskService.delegate(taskId, userId, targetUserId, comment);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "转办成功");
        return response;
    }

    /**
     * 撤回流程
     */
    @PostMapping("/withdraw")
    public Map<String, Object> withdraw(@RequestBody Map<String, Object> params) {
        String processInstanceId = (String) params.get("processInstanceId");
        String userId = (String) params.get("userId");
        
        flowTaskService.withdraw(processInstanceId, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "撤回成功");
        return response;
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{taskId}")
    public Map<String, Object> getById(@PathVariable String taskId) {
        FlowTask task = flowTaskService.getTaskDetail(taskId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", task);
        return response;
    }

    /**
     * 获取流程图
     */
    @GetMapping("/diagram/{processInstanceId}")
    public ResponseEntity<byte[]> getProcessDiagram(@PathVariable String processInstanceId) {
        byte[] diagram = flowTaskService.getProcessDiagram(processInstanceId);
        
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
    public Map<String, Object> remind(@RequestParam String taskId) {
        flowTaskService.remind(taskId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "催办成功");
        return response;
    }
}