/*
 * MIT License
 *
 * Copyright (c) 2024 Hongtao Liu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

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
