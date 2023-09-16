package com.github.iassign.core.dag;

import com.fasterxml.jackson.databind.JsonNode;

public interface DagElementParser {

    public DagElement parse(JsonNode jsonNode);
}
