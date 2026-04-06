package com.mdframe.forge.starter.idempotent.util;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class SpelUtil {
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    public static Object parse(String expression, Object[] args, String[] paramNames) {
        EvaluationContext context = new StandardEvaluationContext();
        if (args != null && paramNames != null) {
            for (int i = 0; i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        Expression exp = PARSER.parseExpression(expression);
        return exp.getValue(context);
    }
}
