package com.github.iassign.core.expression;

import java.util.Map;

/**
 * 可以计算表达式
 */
public interface ExpressionEvaluator {
    Object evaluate(String expression, Map<String, Object> variables) throws Exception;
}
