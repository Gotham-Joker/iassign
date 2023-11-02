package com.github.iassign.service;

import com.github.base.BaseService;
import com.github.core.JsonUtil;
import com.github.iassign.entity.ProcessVariables;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ProcessVariablesService extends BaseService<ProcessVariables> {
    public Map<String, Object> getVariables(String variableId) {
        ProcessVariables processVariables = selectById(variableId);
        return JsonUtil.readValue(processVariables.data, Map.class);
    }

    /**
     * 合并变量到全局变量中
     *
     * @param variableId
     * @param variablesIN
     */
    @Transactional
    public void mergeVariables(String variableId, Map<String, Object> variablesIN) {
        ProcessVariables processVariables = selectById(variableId);
        Map<String, Object> contextVariables = JsonUtil.readValue(processVariables.data, Map.class);
        contextVariables.putAll(variablesIN);
        processVariables.data = JsonUtil.toJson(contextVariables);
        updateById(processVariables);
    }
}
