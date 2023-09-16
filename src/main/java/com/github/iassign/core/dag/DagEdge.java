package com.github.iassign.core.dag;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.iassign.core.dag.node.DagNode;

public class DagEdge extends DagElement {
    public String condition; // 流转条件
    public String source;
    public String target;
    public DagNode sourceNode;
    public DagNode targetNode;

    public DagEdge(JsonNode jsonNode) {
        this.id = jsonNode.get("id").asText("");
        this.source = jsonNode.get("source").get("cell").asText();
        this.target = jsonNode.get("target").get("cell").asText();
        JsonNode dataNode = jsonNode.get("data");
        if (dataNode == null) {
            return;
        }
        JsonNode labelNode = dataNode.get("label");
        this.label = labelNode == null ? "" : labelNode.asText("");
        JsonNode conditionNode = dataNode.get("condition");
        this.condition = conditionNode == null ? null : conditionNode.asText("");
    }

    public DagEdge() {

    }

}
