package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.flow.entity.FlowComment;
import com.mdframe.forge.starter.flow.mapper.FlowCommentMapper;
import com.mdframe.forge.starter.flow.service.FlowCommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 流程审批意见服务实现
 */
@Slf4j
@Service
public class FlowCommentServiceImpl extends ServiceImpl<FlowCommentMapper, FlowComment> implements FlowCommentService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowComment addComment(String processInstanceId, String processDefKey,
                                   String taskId, String taskName, String type,
                                   String message, String userId, String userName) {
        
        FlowComment comment = new FlowComment();
        comment.setProcessInstanceId(processInstanceId);
        comment.setProcessDefKey(processDefKey);
        comment.setTaskId(taskId);
        comment.setTaskName(taskName);
        comment.setType(type != null ? type : "comment");
        comment.setMessage(message);
        comment.setUserId(userId);
        comment.setUserName(userName);
        
        save(comment);
        log.info("添加审批意见：processInstanceId={}, taskId={}, message={}", 
                processInstanceId, taskId, message);
        
        return comment;
    }

    @Override
    public List<FlowComment> getCommentsByProcessInstanceId(String processInstanceId) {
        LambdaQueryWrapper<FlowComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowComment::getProcessInstanceId, processInstanceId)
                .orderByAsc(FlowComment::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<FlowComment> getCommentsByTaskId(String taskId) {
        LambdaQueryWrapper<FlowComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowComment::getTaskId, taskId)
                .orderByAsc(FlowComment::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowComment addEvent(String processInstanceId, String processDefKey,
                                 String message, String userId, String userName) {
        
        FlowComment event = new FlowComment();
        event.setProcessInstanceId(processInstanceId);
        event.setProcessDefKey(processDefKey);
        event.setType("event");
        event.setMessage(message);
        event.setUserId(userId);
        event.setUserName(userName);
        
        save(event);
        log.info("添加流程事件：processInstanceId={}, message={}", processInstanceId, message);
        
        return event;
    }
}