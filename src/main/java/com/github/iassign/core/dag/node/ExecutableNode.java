package com.github.iassign.core.dag.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.authorization.Authentication;
import org.slf4j.Logger;

import java.util.Map;

public class ExecutableNode extends DagNode {
    @JsonIgnore
    public Authentication delegator;

    public ExecutableNode() {
    }

    public ExecutableNode(JsonNode jsonNode) {
        super(jsonNode);
    }

    /**
     * 可以被执行的节点
     *
     * @param logger    日志记录
     * @param variables
     */
    public void execute(Logger logger, Map<String, Object> variables) throws Exception {

    }
}
