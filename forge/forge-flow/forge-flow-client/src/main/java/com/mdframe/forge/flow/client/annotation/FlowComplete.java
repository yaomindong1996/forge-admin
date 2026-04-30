package com.mdframe.forge.flow.client.annotation;

import java.lang.annotation.*;

/**
 * 流程结束回调注解 —— 类级别语义化封装
 * <p>
 * 与 {@link FlowCallback} 的区别：{@code @FlowCallback} 单方法处理所有事件，需在方法内部判断
 * 事件类型；{@code @FlowComplete} 将通过/驳回/取消分发到各自独立的方法，代码更清晰。
 *
 * <h3>使用示例</h3>
 * <pre>
 * {@literal @}FlowBind(modelKey = "leave_process", businessType = "leave")
 * {@literal @}FlowComplete(onApproved = "handleApproved", onRejected = "handleRejected")
 * {@literal @}Service
 * public class LeaveRequestServiceImpl {
 *
 *     public void handleApproved(FlowEventContext ctx) {
 *         // 流程通过：更新请假单状态为已批准
 *     }
 *
 *     public void handleRejected(FlowEventContext ctx) {
 *         // 流程驳回：更新请假单状态为已驳回，发送驳回通知
 *     }
 *
 *     // 无参形式同样支持
 *     public void handleCanceled() {
 *         // 流程取消
 *     }
 * }
 * </pre>
 *
 * <h3>方法签名约定</h3>
 * 目标方法可以是：
 * <ul>
 *   <li>无参：{@code public void handleApproved()}</li>
 *   <li>接收上下文：{@code public void handleApproved(FlowEventContext ctx)}</li>
 * </ul>
 *
 * @author forge
 * @see FlowCallback
 * @see FlowBind
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FlowComplete {

    /**
     * 流程通过（PROCESS_COMPLETED）时调用的方法名。
     * 留空则不处理该事件。
     */
    String onApproved() default "";

    /**
     * 流程驳回（PROCESS_REJECTED）时调用的方法名。
     * 留空则不处理该事件。
     */
    String onRejected() default "";

    /**
     * 流程撤回/取消（PROCESS_CANCELED）时调用的方法名。
     * 留空则不处理该事件。
     */
    String onCanceled() default "";
}
