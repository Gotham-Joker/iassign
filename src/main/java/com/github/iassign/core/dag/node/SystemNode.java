package com.github.iassign.core.dag.node;


import com.fasterxml.jackson.databind.JsonNode;
import com.github.core.JsonUtil;
import com.github.iassign.core.expression.ExpressionEvaluator;
import com.github.iassign.util.PlaceHolderUtil;
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
    public String method;
    public String header;
    public String body;
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
            JsonNode methodNode = data.get("method");
            method = methodNode == null ? null : methodNode.asText("POST");
            JsonNode headerNode = data.get("header");
            header = headerNode == null ? null : headerNode.asText("");
            JsonNode bodyNode = data.get("body");
            body = bodyNode == null ? null : bodyNode.asText("");
            JsonNode conditionNode = data.get("condition");
            condition = conditionNode == null ? null : conditionNode.asText("");
        }
    }

    @Override
    public void execute(Logger logger, Map<String, Object> variables) throws Exception {
        logger.info("variables:{}", variables);
        // 发送HTTP
        if (StringUtils.hasText(url)) {
            if (url.contains("${")) {
                url = PlaceHolderUtil.replace(url, variables);
            }
            logger.info("http method:{},url:{}", method, url);
            RequestConfig config = RequestConfig.custom()
                    .setConnectionRequestTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                    .setResponseTimeout(socketTimeout, TimeUnit.MILLISECONDS).build();
            ClassicRequestBuilder builder;
            switch (method) {
                case "GET":
                    builder = ClassicRequestBuilder.get(url);
                    break;
                case "PUT":
                    builder = ClassicRequestBuilder.put(url);
                    break;
                case "DELETE":
                    builder = ClassicRequestBuilder.delete(url);
                    break;
                case "POST":
                default:
                    builder = ClassicRequestBuilder.post(url);
                    break;
            }
            if (StringUtils.hasText(header)) {
                for (String h : header.split("\n")) {
                    String[] kv = h.split(":");
                    if (kv.length > 0) {
                        builder.setHeader(kv[0], kv[1].trim());
                    }
                }
                logger.info("http header:{}", header);
            }
            if (StringUtils.hasText(body)) {
                String tmpBody = PlaceHolderUtil.replace(body, variables);
                logger.info("http body:{}", tmpBody);
                builder.setEntity(tmpBody);
            }
            ClassicHttpRequest request = builder.build();
            while (retry >= 0) {
                try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(config).build()) {
                    String result = httpClient.execute(request, resp -> {
                        if (resp.getCode() != 200) {
                            logger.error("http status code[{}] != 200,ERROR", resp.getCode());
                        }
                        return EntityUtils.toString(resp.getEntity());
                    });
                    logger.info("http result: {}", result);
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
