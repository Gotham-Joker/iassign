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
