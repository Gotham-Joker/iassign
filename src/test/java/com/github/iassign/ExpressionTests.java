package com.github.iassign;

import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.config.QLExpressRunStrategy;
import org.junit.jupiter.api.Test;

public class ExpressionTests {


    /**
     * 表达式测试
     */
    @Test
    public void testExpression() throws Exception {
        QLExpressRunStrategy.setForbidInvokeSecurityRiskMethods(true);
        ExpressRunner runner = new ExpressRunner();
        DefaultContext<String, Object> context = new DefaultContext<>();
        context.put("type", "001-测试");
        String expression = "type.startsWith('001')";
        Object result = runner.execute(expression, context, null, true, false);
        System.out.println(result);
    }
}
