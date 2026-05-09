package com.mdframe.forge.starter.flow.listener;

import com.mdframe.forge.starter.flow.event.FlowModelPublishEvent;
import com.mdframe.forge.starter.flow.service.FlowModelVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 流程模型发布事件监听器
 * 监听模型发布事件，自动插入版本历史记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowModelPublishEventListener {
    
    private final FlowModelVersionService flowModelVersionService;
    
    @EventListener
    public void onFlowModelPublish(FlowModelPublishEvent event) {
        try {
            flowModelVersionService.insertVersionOnPublish(event.getModel(), event.getChangeDescription());
            log.info("版本历史记录插入成功：modelId={}", event.getModel().getId());
        } catch (Exception e) {
            log.error("版本历史记录插入失败：modelId={}", event.getModel().getId(), e);
        }
    }
}