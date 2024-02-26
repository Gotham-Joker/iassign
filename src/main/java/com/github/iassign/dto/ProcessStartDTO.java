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

package com.github.iassign.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/**
 * 包装着启动流程实例所需的参数
 */
public class ProcessStartDTO {
    // 需要启动的流程定义ID
    @NotEmpty(message = "请提供流程定义ID")
    public String definitionId;
    // 流程变量
    public Map<String, Object> variables;
    // 表单数据，里面包含表单的渲染信息，不能直接拿来当流程变量
    // 需要特殊处理然后转换为流程变量
    public Map<String, Object> formData;
    // 流程发起人的ID
    public String starter;
    // 邮件接收人
    public String emails;
    public String instanceId;
}
