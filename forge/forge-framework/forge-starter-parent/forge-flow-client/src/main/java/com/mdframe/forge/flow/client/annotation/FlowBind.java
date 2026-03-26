package com.mdframe.forge.flow.client.annotation;

import java.lang.annotation.*;

/**
 * 流程绑定注解 —— 类级别
 * <p>
 * 标注在业务 Service 类上，声明该业务类绑定的流程模型 Key。
 * 与 {@link FlowStart} 配合使用时可省略方法上的 modelKey 属性。
 *
 * <pre>
 * {@literal @}FlowBind(modelKey = "leave_process", businessType = "leave")
 * {@literal @}Service
 * public class LeaveService {
 *
 *     {@literal @}FlowStart(
 *         businessKeySpEl = "'leave:' + #leave.id",
 *         titleSpEl       = "#leave.applicantName + ' 的请假申请'",
 *         userIdSpEl      = "#leave.applicantId",
 *         userNameSpEl    = "#leave.applicantName"
 *     )
 *     public LeaveApply submit(LeaveApply leave) { ... }
 * }
 * </pre>
 *
 * @author forge
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FlowBind {

    /** 流程模型 Key（对应 sys_flow_model.model_key） */
    String modelKey();

    /** 业务类型（可选，用于分类） */
    String businessType() default "";
}
