package com.github.iassign.core.dag.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.iassign.core.expression.ExpressionEvaluator;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class UserTaskNode extends DagNode implements ExpressionNode {
    public String status;
    public String formId;
    public List<String> userList;
    public List<String> roleList;
    public String userScript;
    public String roleScript;
    private ExpressionEvaluator expressionEvaluator;


    public UserTaskNode() {
    }

    public UserTaskNode(JsonNode jsonNode) {
        super(jsonNode);
        JsonNode data = jsonNode.get("data");
        JsonNode formIdNode = data.get("formId");
        if (formIdNode != null) {
            this.formId = formIdNode.asText("");
        }
        this.userScript = data.get("userScript").asText("");
        this.roleScript = data.get("roleScript").asText("");
        userList = new ArrayList<>();
        roleList = new ArrayList<>();
        ArrayNode userListNode = (ArrayNode) data.get("userList");
        ArrayNode roleListNode = (ArrayNode) data.get("roleList");
        userListNode.forEach(node -> userList.add(node.asText()));
        roleListNode.forEach(node -> roleList.add(node.asText()));
    }


    /**
     * 获取审批候选人
     *
     * @param variables
     */
    public List<String> candidateUsers(Map<String, Object> variables) {
        return candidates(variables, userScript, userList);
    }


    /**
     * 获取审批候选角色
     *
     * @param variables
     */
    public List<String> candidateRoles(Map<String, Object> variables) {
        return candidates(variables, roleScript, roleList);
    }

    private List<String> candidates(Map<String, Object> variables,
                                    String expression, List<String> defaultList) {
        List<String> list = null;
        if (StringUtils.hasText(expression)) {
            Object result;
            try {
                result = expressionEvaluator.evaluate(expression, variables);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (result instanceof String) {
                list = Collections.singletonList((String) result);
            } else if (result instanceof List) {
                list = (List<String>) result;
            }
            return list;
        }
        return defaultList;
    }

    @Override
    public void setExpression(ExpressionEvaluator expressionEvaluator) {
        this.expressionEvaluator = expressionEvaluator;
    }
}
