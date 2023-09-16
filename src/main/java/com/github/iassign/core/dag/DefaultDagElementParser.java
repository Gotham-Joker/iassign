package com.github.iassign.core.dag;


import com.fasterxml.jackson.databind.JsonNode;
import com.github.iassign.core.dag.node.*;

public class DefaultDagElementParser implements DagElementParser {


    @Override
    public DagElement parse(JsonNode jsonNode) {
        String shape = jsonNode.get("shape").asText("");
        switch (shape) {
            case "dag-edge":
                return new DagEdge(jsonNode);
            case "start-node":
                return new StartNode(jsonNode);
            case "end-node":
                return new EndNode(jsonNode);
            case "gateway-node":
                return new GatewayNode(jsonNode);
            case "user-task-node":
                return new UserTaskNode(jsonNode);
            case "system-node":
                return new SystemNode(jsonNode);
            default:
                return null;
        }
    }
}
