package com.mdframe.forge.flow.client.helper;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.flow.client.annotation.FlowBind;
import com.mdframe.forge.flow.client.annotation.FlowStart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link FlowStart} 注解 AOP 切面（业务侧）
 * <p>
 * 业务方法执行成功后，自动通过 {@link FlowClient} 发起远程流程，
 * 无需业务代码侵入。
 * <p>
 * 由 {@link FlowClientHelperAutoConfiguration} 自动注册，
 * 只要引入 {@code forge-flow-client} 并配置好 {@code forge.flow.client.url} 即可生效。
 *
 * @author forge
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class FlowStartAspect {

    private final FlowClient flowClient;
    private final ApplicationContext applicationContext;

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    @Around("@annotation(flowStart)")
    public Object around(ProceedingJoinPoint pjp, FlowStart flowStart) throws Throwable {
        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable ex) {
            if (!flowStart.skipOnError()) {
                log.warn("[FlowStart] 业务方法异常，尝试发起流程: {}", ex.getMessage());
                tryStart(pjp, flowStart, null);
            }
            throw ex;
        }
        tryStart(pjp, flowStart, result);
        return result;
    }

    private void tryStart(ProceedingJoinPoint pjp, FlowStart flowStart, Object result) {
        try {
            // 解析 modelKey
            String modelKey = flowStart.modelKey();
            if (!StringUtils.hasText(modelKey)) {
                FlowBind bind = resolveFlowBind(pjp);
                if (bind != null) {
                    modelKey = bind.modelKey();
                }
            }
            if (!StringUtils.hasText(modelKey)) {
                log.warn("[FlowStart] 未找到 modelKey（@FlowStart 未指定且类上无 @FlowBind），跳过");
                return;
            }

            // 解析业务类型
            String businessType = "";
            FlowBind bind = resolveFlowBind(pjp);
            if (bind != null) {
                businessType = bind.businessType();
            }

            EvaluationContext ctx = buildSpelContext(pjp, result);

            String businessKey = eval(flowStart.businessKeySpEl(), ctx, String.class);
            String title       = eval(flowStart.titleSpEl(),       ctx, String.class);
            String userId      = eval(flowStart.userIdSpEl(),      ctx, String.class);
            String userName    = eval(flowStart.userNameSpEl(),    ctx, String.class);
            String deptId      = eval(flowStart.deptIdSpEl(),      ctx, String.class);
            String deptName    = eval(flowStart.deptNameSpEl(),    ctx, String.class);

            Map<String, Object> variables = new HashMap<>();
            if (StringUtils.hasText(flowStart.variablesSpEl())) {
                @SuppressWarnings("unchecked")
                Map<String, Object> extra = eval(flowStart.variablesSpEl(), ctx, Map.class);
                if (extra != null) {
                    variables.putAll(extra);
                }
            }

            if (!StringUtils.hasText(businessKey)) {
                log.warn("[FlowStart] businessKeySpEl 计算为空，跳过流程发起");
                return;
            }

            FlowResult<String> res = flowClient.startProcess(
                    modelKey, businessKey, businessType,
                    title, variables,
                    userId, userName, deptId, deptName);

            if (res.isSuccess()) {
                log.info("[FlowStart] 流程发起成功 modelKey={} businessKey={} instanceId={}",
                        modelKey, businessKey, res.getData());
            } else {
                log.error("[FlowStart] 流程发起失败 modelKey={} businessKey={} msg={}",
                        modelKey, businessKey, res.getMsg());
            }

        } catch (Exception e) {
            log.error("[FlowStart] 自动发起流程异常: {}", e.getMessage(), e);
        }
    }

    private FlowBind resolveFlowBind(ProceedingJoinPoint pjp) {
        Class<?> clazz = pjp.getTarget().getClass();
        FlowBind bind = clazz.getAnnotation(FlowBind.class);
        if (bind == null && clazz.getSuperclass() != null) {
            bind = clazz.getSuperclass().getAnnotation(FlowBind.class);
        }
        return bind;
    }

    private EvaluationContext buildSpelContext(ProceedingJoinPoint pjp, Object result) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        Parameter[] params = ((MethodSignature) pjp.getSignature()).getMethod().getParameters();
        Object[] args = pjp.getArgs();
        for (int i = 0; i < params.length; i++) {
            ctx.setVariable(params[i].getName(), args[i]);
            ctx.setVariable("p" + i, args[i]);
        }
        if (result != null) {
            ctx.setVariable("result", result);
        }
        ctx.setBeanResolver((context, beanName) -> applicationContext.getBean(beanName));
        return ctx;
    }

    private <T> T eval(String spel, EvaluationContext ctx, Class<T> type) {
        if (!StringUtils.hasText(spel)) {
            return null;
        }
        try {
            return PARSER.parseExpression(spel).getValue(ctx, type);
        } catch (Exception e) {
            log.warn("[FlowStart] SpEL 解析失败 '{}': {}", spel, e.getMessage());
            return null;
        }
    }
}
