package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.flow.entity.FlowCc;
import com.mdframe.forge.starter.flow.mapper.FlowCcMapper;
import com.mdframe.forge.starter.flow.service.FlowCcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程抄送服务实现
 */
@Slf4j
@Service
public class FlowCcServiceImpl extends ServiceImpl<FlowCcMapper, FlowCc> implements FlowCcService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendCc(String processInstanceId, String processDefKey, String taskId,
                       String title, String content, String businessKey,
                       List<String> ccUserIds, List<String> ccUserNames,
                       String sendUserId, String sendUserName) {
        
        if (ccUserIds == null || ccUserIds.isEmpty()) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < ccUserIds.size(); i++) {
            FlowCc cc = new FlowCc();
            cc.setProcessInstanceId(processInstanceId);
            cc.setProcessDefKey(processDefKey);
            cc.setTaskId(taskId);
            cc.setTitle(title);
            cc.setContent(content);
            cc.setBusinessKey(businessKey);
            cc.setCcUserId(ccUserIds.get(i));
            cc.setCcUserName(ccUserNames != null && i < ccUserNames.size() ? ccUserNames.get(i) : null);
            cc.setSendUserId(sendUserId);
            cc.setSendUserName(sendUserName);
            cc.setCcTime(now);
            cc.setIsRead(0);
            
            save(cc);
        }
        
        log.info("发送抄送：processInstanceId={}, ccUserIds={}", processInstanceId, ccUserIds);
    }

    @Override
    public IPage<FlowCc> myCc(Page<FlowCc> page, String userId, Integer isRead) {
        LambdaQueryWrapper<FlowCc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowCc::getCcUserId, userId)
                .eq(isRead != null, FlowCc::getIsRead, isRead)
                .orderByDesc(FlowCc::getCcTime);
        return page(page, wrapper);
    }

    @Override
    public IPage<FlowCc> sentCc(Page<FlowCc> page, String userId) {
        LambdaQueryWrapper<FlowCc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowCc::getSendUserId, userId)
                .orderByDesc(FlowCc::getCcTime);
        return page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(String id) {
        LambdaUpdateWrapper<FlowCc> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FlowCc::getId, id)
                .set(FlowCc::getIsRead, 1)
                .set(FlowCc::getReadTime, LocalDateTime.now());
        update(wrapper);
        log.info("标记抄送已读：{}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMarkRead(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        LambdaUpdateWrapper<FlowCc> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(FlowCc::getId, ids)
                .set(FlowCc::getIsRead, 1)
                .set(FlowCc::getReadTime, LocalDateTime.now());
        update(wrapper);
        log.info("批量标记抄送已读：{}条", ids.size());
    }

    @Override
    public long countUnread(String userId) {
        LambdaQueryWrapper<FlowCc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowCc::getCcUserId, userId)
                .eq(FlowCc::getIsRead, 0);
        return count(wrapper);
    }
}