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
