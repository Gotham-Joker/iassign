package com.github.iassign.core.dag.node;


import com.fasterxml.jackson.databind.JsonNode;
import com.github.core.JsonUtil;
import com.github.iassign.core.expression.ExpressionEvaluator;
import lombok.Getter;
import lombok.Setter;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;


@Getter
@Setter
public class SystemNode extends ExecutableNode implements ExpressionNode {
    public String script;
    public Integer connectTimeout;
    public Integer socketTimeout;
    public Integer retry;
    public String url;
    public String condition;
    private ExpressionEvaluator expressionEvaluator;

    public SystemNode() {

    }

    public SystemNode(JsonNode jsonNode) {
        super(jsonNode);
        JsonNode data = jsonNode.get("data");
        if (data != null) {
            JsonNode scriptNode = data.get("script");
            script = scriptNode == null ? null : scriptNode.asText("");
            JsonNode connectTimeoutNode = data.get("connectTimeout");
            this.connectTimeout = connectTimeoutNode == null ? 0 : connectTimeoutNode.asInt(0);
            JsonNode socketTimeoutNode = data.get("socketTimeout");
            socketTimeout = socketTimeoutNode == null ? 0 : socketTimeoutNode.asInt(0);
            JsonNode retryNode = data.get("retry");
            retry = retryNode == null ? 0 : retryNode.asInt(0);
            JsonNode urlNode = data.get("url");
            url = urlNode == null ? null : urlNode.asText("");
            JsonNode conditionNode = data.get("condition");
            condition = conditionNode == null ? null : conditionNode.asText("");
        }
    }

    @Override
    public void execute(Logger logger, Map<String, Object> variables) throws Exception {
        // 发送HTTP
        if (StringUtils.hasText(url)) {
            RequestConfig config = RequestConfig.custom()
                    .setConnectionRequestTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                    .setResponseTimeout(socketTimeout, TimeUnit.MILLISECONDS).build();

            while (retry >= 0) {
                try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(config).build()) {
                    String json = JsonUtil.toJson(variables);
                    logger.info("发送http post json请求: {}，json对象： {}", url, json);
                    ClassicHttpRequest post = ClassicRequestBuilder.post(url)
                            .setEntity(new StringEntity(json, ContentType.APPLICATION_JSON)).build();
                    String result = httpClient.execute(post, resp -> EntityUtils.toString(resp.getEntity()));
                    logger.info("http返回值: {}", result);
                    if (StringUtils.hasText(condition)) {
                        if (!result.contains(condition)) {
                            // 失败
                            logger.error("返回值未通过校验(返回内容未包含字符串:{})，即将抛出异常", condition);
                            throw new RuntimeException("unsatisfied condition,result:" + result);
                        }
                    }
                    break;
                } catch (Exception e) {
                    retry--;
                    if (retry < 0) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        // 执行脚本
        if (StringUtils.hasText(script)) {
            this.expressionEvaluator.evaluate(script, variables);
        }
    }


    @Override
    public void setExpression(ExpressionEvaluator expressionEvaluator) {
        this.expressionEvaluator = expressionEvaluator;
    }
}
