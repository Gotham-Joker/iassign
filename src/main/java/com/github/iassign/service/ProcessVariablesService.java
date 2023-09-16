package com.github.iassign.service;

import com.github.base.BaseService;
import com.github.core.JsonUtil;
import com.github.iassign.entity.ProcessVariables;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ProcessVariablesService extends BaseService<ProcessVariables> {
    public Map<String, Object> getVariables(String variableId) {
        ProcessVariables processVariables = selectById(variableId);
        return JsonUtil.readValue(processVariables.data, Map.class);
    }
}
