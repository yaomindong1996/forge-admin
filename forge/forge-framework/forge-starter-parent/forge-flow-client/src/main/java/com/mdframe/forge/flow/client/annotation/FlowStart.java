package com.mdframe.forge.flow.client.annotation;

import java.lang.annotation.*;

/**
 * 自动发起流程注解 —— 方法级别
 * <p>
 * 标注在业务方法上，方法执行成功后由切面自动通过 {@link com.mdframe.forge.flow.client.FlowClient}
 * 发起流程，无需手动调用。
 *
 * <h3>SpEL 上下文变量</h3>
 * <ul>
 *   <li>{@code #paramName} — 方法参数（按参数名）</li>
 *   <li>{@code #p0}, {@code #p1} — 方法参数（按位置）</li>
 *   <li>{@code #result} — 方法返回值</li>
 * </ul>
 *
 * <h3>示例</h3>
 * <pre>
 * {@literal @}FlowStart(
 *     modelKey        = "leave_process",          // 可省略，取 @FlowBind.modelKey
 *     businessKeySpEl = "'leave:' + #leave.id",
 *     titleSpEl       = "#leave.name + ' 的请假申请'",
 *     userIdSpEl      = "#leave.applicantId",
 *     userNameSpEl    = "#leave.applicantName",
 *     variablesSpEl   = "{'days': #leave.days}"
 * )
 * public LeaveApply submit(LeaveApply leave) { ... }
 * </pre>
 *
 * @author forge
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FlowStart {

    /** 流程模型 Key，不填则取类上 {@link FlowBind#modelKey()} */
    String modelKey() default "";

    /** 业务唯一标识 SpEL（必填），如 {@code "'leave:' + #leave.id"} */
    String businessKeySpEl();

    /** 流程标题 SpEL，如 {@code "#leave.name + ' 的请假申请'"} */
    String titleSpEl() default "";

    /** 发起人 ID SpEL */
    String userIdSpEl() default "";

    /** 发起人姓名 SpEL */
    String userNameSpEl() default "";

    /** 部门 ID SpEL（可选） */
    String deptIdSpEl() default "";

    /** 部门名称 SpEL（可选） */
    String deptNameSpEl() default "";

    /**
     * 额外流程变量 SpEL，需返回 {@code Map<String,Object>}
     * 示例：{@code "{'days': #leave.days, 'reason': #leave.reason}"}
     */
    String variablesSpEl() default "";

    /** 业务方法异常时是否跳过流程发起（默认 true） */
    boolean skipOnError() default true;
}
