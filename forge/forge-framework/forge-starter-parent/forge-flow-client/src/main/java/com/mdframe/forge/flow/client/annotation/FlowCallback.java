package com.mdframe.forge.flow.client.annotation;

import java.lang.annotation.*;

/**
 * 流程事件回调注解 —— 方法级别
 * <p>
 * 标注在业务 Service 方法上，当 flow-server 通过 <b>Redis Pub/Sub</b> 或 <b>Webhook</b>
 * 推送流程事件时，由 {@link com.mdframe.forge.flow.client.helper.FlowEventSubscriber}
 * 自动路由并调用此方法。
 *
 * <h3>方法签名约定（二选一）</h3>
 * <pre>
 * // 方式1：接收完整上下文
 * {@literal @}FlowCallback(on = {FlowCallback.ON_COMPLETED, FlowCallback.ON_REJECTED})
 * public void onFlowResult(FlowEventContext ctx) { ... }
 *
 * // 方式2：无参（只关心触发，不需要详情）
 * {@literal @}FlowCallback(on = FlowCallback.ON_COMPLETED)
 * public void onCompleted() { ... }
 * </pre>
 *
 * @author forge
 * @see FlowBind
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FlowCallback {

    /** 流程通过事件 */
    String ON_COMPLETED = "PROCESS_COMPLETED";
    /** 流程驳回事件 */
    String ON_REJECTED  = "PROCESS_REJECTED";
    /** 流程撤回/取消事件 */
    String ON_CANCELED  = "PROCESS_CANCELED";
    /** 任务创建事件（新待办产生） */
    String ON_TASK_CREATED   = "TASK_CREATED";
    /** 任务完成事件（某个审批节点处理完） */
    String ON_TASK_COMPLETED = "TASK_COMPLETED";
    /** 任务分配/签收事件 */
    String ON_TASK_ASSIGNED  = "TASK_ASSIGNED";

    /**
     * 触发此回调的事件类型（默认监听所有结束事件）
     */
    String[] on() default { ON_COMPLETED, ON_REJECTED, ON_CANCELED };
}
