package com.mdframe.forge.starter.flow.event;

import com.mdframe.forge.starter.flow.entity.FlowModel;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 流程模型发布事件
 * 用于在模型发布成功后触发版本历史记录插入
 * 解决 Service 循环依赖问题
 */
@Getter
public class FlowModelPublishEvent extends ApplicationEvent {
    
    private final FlowModel model;
    private final String changeDescription;
    
    public FlowModelPublishEvent(Object source, FlowModel model, String changeDescription) {
        super(source);
        this.model = model;
        this.changeDescription = changeDescription;
    }
}