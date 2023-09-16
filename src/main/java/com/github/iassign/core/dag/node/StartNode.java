package com.github.iassign.core.dag.node;


import com.fasterxml.jackson.databind.JsonNode;

public class StartNode extends DagNode {
    public StartNode() {

    }

    public StartNode(JsonNode jsonNode) {
        super(jsonNode);
        this.label = "start";
    }
}
