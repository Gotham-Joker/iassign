package com.github.iassign.core.expression;

import com.github.core.ApiException;
import com.github.iassign.Constants;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.config.QLExpressRunStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
public class DefaultExpressionEvaluator implements ExpressionEvaluator {
    private final ExpressRunner runner;

    public DefaultExpressionEvaluator() {
        this(null);
    }

    public DefaultExpressionEvaluator(ApplicationContext context) {
        // 禁止不安全的api调用
        QLExpressRunStrategy.setForbidInvokeSecurityRiskMethods(true);
        this.runner = new ExpressRunner();
        if (context != null) {
            try {
                this.runner.addFunctionOfServiceMethod("bean", context,
                        "getBean", new Class[]{String.class}, "获取bean失败");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Object evaluate(String rawExpression, Map<String, Object> variables) throws Exception {
        String expression = validate(rawExpression);
        DefaultContext<String, Object> context = new DefaultContext<>();
        context.putAll(variables);
        context.put(Constants.PROCESS_CONTEXT, new HashMap<String, Object>());
        Object result = runner.execute(expression, context, null, true, false);
        Map<String, Object> variablesIN = (HashMap<String, Object>) context.get(Constants.PROCESS_CONTEXT);
        if (!CollectionUtils.isEmpty(variablesIN)) {
            variables.put(Constants.PROCESS_CONTEXT, variablesIN);
        }
        return result;
    }


    /**
     * 运行前的校验，过滤一些危险的关键字和代码注释
     *
     * @param expression
     */
    private String validate(String expression) throws Exception {
        if (Stream.of("class", "Class", "import").anyMatch(expression::contains)) {
            throw new ApiException(500, "script contains a risk operation");
        }
        // no comment expression
        String ncExpression = expression.replaceAll("(//.*\n)|(/\\*[\\S\\s]+?\\*/)", "");
        runner.parseInstructionSet(ncExpression);
        return ncExpression;
    }

    public void clear() {
        runner.clearExpressCache();
    }

}
