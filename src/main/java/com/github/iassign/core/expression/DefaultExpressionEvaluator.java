package com.github.iassign.core.expression;

import com.github.core.ApiException;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.config.QLExpressRunStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.Map;
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

    public Object evaluate(String expression, Map<String, Object> variables) throws Exception {
        validate(expression);
        DefaultContext<String, Object> context = new DefaultContext<>();
        context.putAll(variables);

        return runner.execute(expression, context, null, true, false);
    }

    /**
     * 运行前的校验，过滤一些危险的关键字
     *
     * @param expression
     */
    private void validate(String expression) throws Exception {
        if (Stream.of("class", "Class", "import").anyMatch(expression::contains)) {
            throw new ApiException(500, "script contains a risk operation");
        }
        runner.parseInstructionSet(expression);
    }

    public void clear() {
        runner.clearExpressCache();
    }

}
