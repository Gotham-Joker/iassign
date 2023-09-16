package com.github.iassign.core.dag.node;


import com.fasterxml.jackson.databind.JsonNode;

public class EndNode extends DagNode {
    public EndNode() {

    }

    public EndNode(JsonNode jsonNode) {
        super(jsonNode);
        this.label = "end";
    }
}
