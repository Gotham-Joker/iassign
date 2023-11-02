package com.github.iassign.core.dag.node;


import com.fasterxml.jackson.databind.JsonNode;
import com.github.core.ApiException;
import com.github.core.JsonUtil;
import com.github.iassign.Constants;
import com.github.iassign.core.expression.ExpressionEvaluator;
import com.github.iassign.core.util.PlaceHolderUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


@Getter
@Setter
public class SystemNode extends ExecutableNode implements ExpressionNode {
    public String script;
    public Integer connectTimeout;
    public Integer socketTimeout;
    public Integer retry;
    public Integer delay;
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
            JsonNode delayNode = data.get("delay");
            delay = delayNode == null ? 5000 : Math.max(delayNode.asInt(5000), 5000);
            JsonNode urlNode = data.get("url");
            url = urlNode == null ? null : urlNode.asText("");
            JsonNode methodNode = data.get("method");
            method = methodNode == null ? null : methodNode.asText("GET");
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
        // http返回值
        AtomicReference<Object> HTTP_RESULT = new AtomicReference<>(null);
        // 发送HTTP
        if (StringUtils.hasText(url)) {
            if (url.startsWith("/")) { // 内部访问
                url = "http://127.0.0.1:8080" + url;
            }
            if (url.contains("${")) {
                url = PlaceHolderUtils.replace(url, variables);
            }
            url = url.trim(); // 消除前后空格
            logger.info("http method: {}, url: {}", method, url);
            RequestConfig config = RequestConfig.custom()
                    .setConnectionRequestTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                    .setResponseTimeout(socketTimeout, TimeUnit.MILLISECONDS).build();
            ClassicRequestBuilder builder;
            switch (method) {
                case "GET":
                    builder = ClassicRequestBuilder.get(url);
                    break;
                case "DELETE":
                    builder = ClassicRequestBuilder.delete(url);
                    break;
                case "PUT":
                    builder = ClassicRequestBuilder.put(url);
                    break;
                case "POST":
                default:
                    builder = ClassicRequestBuilder.post(url);
                    break;
            }
            String contentType = "application/json";
            String charset = "utf-8";
            if (StringUtils.hasText(header)) {
                for (String h : header.split("\n")) {
                    String[] kv = h.split(":");
                    if (kv.length > 0) {
                        builder.setHeader(kv[0], kv[1].trim());
                        if (kv[0].toLowerCase().startsWith("application/json")) {
                            String[] split = kv[1].split(";charset=");
                            contentType = split[0];
                            charset = split.length > 1 ? split[1] : charset;
                        }
                    }
                }
                logger.info("http header:{}", builder.getHeaders());
            }
            if (StringUtils.hasText(body)) {
                String tmpBody = PlaceHolderUtils.replace(body, variables);
                logger.info("http body:{}", tmpBody);
                builder.setEntity(new StringEntity(tmpBody, ContentType.create(contentType, charset)));
            }
            ClassicHttpRequest request = builder.setCharset(StandardCharsets.UTF_8).build();
            while (retry >= 0) {
                try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(config).build()) {
                    String result = httpClient.execute(request, resp -> {
                        logger.info("http status code:{}", resp.getCode());
                        if (resp.getCode() != 200) {
                            logger.error("http status code != 200, error");
                            throw new ApiException(500, "http 接口调用失败");
                        }
                        String rs = EntityUtils.toString(resp.getEntity());
                        Header contentTypeHeader = resp.getFirstHeader(HttpHeaders.CONTENT_TYPE);
                        if (contentTypeHeader != null) {
                            String respContentType = contentTypeHeader.getValue();
                            if (respContentType.startsWith("application/json")) {
                                HTTP_RESULT.set(JsonUtil.readValue(rs, Map.class));
                            } else {
                                HTTP_RESULT.set(rs);
                            }
                        }
                        return rs;
                    });
                    logger.info("http result: {}", HTTP_RESULT.get());
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
                    Thread.sleep(delay);
                }
            }
        }
        // 执行脚本
        if (StringUtils.hasText(script)) {
            if (HTTP_RESULT.get() != null) { // 先尝试放入http返回值
                variables.put(Constants.HTTP_RESULT, HTTP_RESULT);
            }
            this.expressionEvaluator.evaluate(script, variables);
            variables.remove(Constants.HTTP_RESULT);
        }
    }


    @Override
    public void setExpression(ExpressionEvaluator expressionEvaluator) {
        this.expressionEvaluator = expressionEvaluator;
    }
}
