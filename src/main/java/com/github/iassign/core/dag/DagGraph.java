package com.github.iassign.core.dag;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.core.ApiException;
import com.github.iassign.core.dag.node.DagNode;
import com.github.iassign.core.dag.node.EndNode;
import com.github.iassign.core.dag.node.StartNode;
import com.github.iassign.core.dag.node.UserTaskNode;
import com.github.iassign.core.expression.DefaultExpressionEvaluator;
import com.github.iassign.core.dag.node.ExpressionNode;
import com.github.iassign.core.expression.ExpressionEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 有向无环图，简称DAG，但是暂时未校验是否存在环
 */
@Slf4j
public class DagGraph {
    public StartNode startNode;
    public final Map<String, DagElement> elements = new HashMap<>();
    private final List<DagEdge> dagEdges = new ArrayList<>();
    private static final Map<String, DagElementParser> PARSER_MAP = new HashMap<>();

    /**
     * 注册默认的转换器，用于解析内置的dag元素
     * 包括"开始"，"结束"，"连接线"，"网关"，"用户审批"，"系统节点"
     */
    static {
        DefaultDagElementParser parser = new DefaultDagElementParser();
        Stream.of("start-node", "end-node", "dag-edge", "gateway-node", "user-task-node", "system-node")
                .forEach(nodeType -> registry(nodeType, parser));
    }

    private ExpressionEvaluator expressionEvaluator;


    public static void registry(String nodeType, DagElementParser parser) {
        PARSER_MAP.put(nodeType, parser);
    }

    public static DagGraph init(ArrayNode arrayNode) {
        return init(arrayNode, new DefaultExpressionEvaluator());
    }

    /**
     * 解析json数组
     *
     * @param arrayNode
     * @return
     */
    public static DagGraph init(ArrayNode arrayNode, ExpressionEvaluator expressionEvaluator) {
        DagGraph dagGraph = new DagGraph();
        dagGraph.expressionEvaluator = expressionEvaluator;
        arrayNode.forEach(node -> {
            String nodeType = node.get("shape").asText("");
            DagElementParser parser = PARSER_MAP.get(nodeType);
            if (parser == null) {
                throw new ApiException(500, "no parser for shape: " + nodeType);
            }
            DagElement dagElement = parser.parse(node);
            String id = node.get("id").asText();
            dagGraph.elements.put(id, dagElement);
            // 放入表达式计算器
            if (dagElement instanceof ExpressionNode) {
                ((ExpressionNode) dagElement).setExpression(expressionEvaluator);
            }
            if (dagElement instanceof StartNode) { // 开始节点
                dagGraph.startNode = (StartNode) dagElement;
            } else if (dagElement instanceof DagEdge) {
                dagGraph.dagEdges.add((DagEdge) dagElement);
            }
        });
        resolveDependencies(dagGraph);
        return dagGraph;
    }

    /**
     * 解析dag图，完善关系
     *
     * @param dagGraph
     */
    private static void resolveDependencies(DagGraph dagGraph) {
        dagGraph.dagEdges.forEach(edge -> {
            DagNode sourceNode = dagGraph.obtainDagNode(edge.source);
            DagNode targetNode = dagGraph.obtainDagNode(edge.target);
            sourceNode.outcomes.add(edge);
            targetNode.incomes.add(edge);
            edge.sourceNode = sourceNode;
            edge.targetNode = targetNode;
        });
    }


    public DagEdge route(String currentNodeId, Map<String, Object> variables) {
        if (currentNodeId == null) {
            return null;
        }
        DagEdge defaultOutcome = null;
        DagNode dagNode = obtainDagNode(currentNodeId);
        for (int i = 0; i < dagNode.outcomes.size(); i++) {
            DagEdge dagEdge = dagNode.outcomes.get(i);
            if (!StringUtils.hasText(dagEdge.condition)) {
                defaultOutcome = dagEdge;
            } else {
                Object result;
                try {
                    result = this.expressionEvaluator.evaluate(dagEdge.condition, variables);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (!(result instanceof Boolean)) {
                    throw new ApiException(500, "should return boolean result");
                }
                if (Boolean.TRUE.equals(result)) {
                    return dagEdge;
                }
            }
        }
        // 非结束节点必须有出口连线
        if (defaultOutcome == null && !(dagNode instanceof EndNode)) {
            throw new ApiException(500, "a dag node must have a default outcome");
        }
        return defaultOutcome;
    }

    /**
     * 获取用户节点
     *
     * @param dagNodeId
     * @return
     */
    public UserTaskNode obtainUserTaskNode(String dagNodeId) {
        return (UserTaskNode) elements.get(dagNodeId);
    }

    /**
     * 获取节点
     *
     * @param dagNodeId
     * @return
     */
    public DagNode obtainDagNode(String dagNodeId) {
        return (DagNode) elements.get(dagNodeId);
    }
}
