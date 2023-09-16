package com.github.iassign.core.dag.node;


import com.fasterxml.jackson.databind.JsonNode;

public class GatewayNode extends DagNode {
    public GatewayNode() {

    }

    public GatewayNode(JsonNode jsonNode) {
        super(jsonNode);
        this.label = "gateway";
    }

}
