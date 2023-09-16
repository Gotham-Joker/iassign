package com.github.iassign.core.dag.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.iassign.core.dag.DagEdge;
import com.github.iassign.core.dag.DagElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DagNode extends DagElement {
    public List<DagEdge> incomes = new ArrayList<>();
    public List<DagEdge> outcomes = new ArrayList<>();

    public DagNode() {
    }

    public DagNode(JsonNode jsonNode) {
        this.id = jsonNode.get("id").asText();
        JsonNode data = jsonNode.get("data");
        if (data != null) {
            this.label = data.get("label").asText("");
        }
    }
}
