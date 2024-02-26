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

package com.github.iassign.vo;

import com.github.iassign.entity.ProcessDefinition;
import lombok.Data;

import java.util.List;

@Data
public class ProcessDefinitionDetailVO {
    private String fallback;
    public String id;
    public String name;
    public String seqNo;
    public String icon;
    // 不是数据库字段
    public String description;
    public Boolean status; // 部署状态 0-未部署 1-已部署
    public String ruId; // 一直保存着最新的运行时dag Id
    public String formId;  // 绑定的表单ID
    public String creator; // 创建人
    public String groupName;
    public String managers;
    private Boolean returnable; // 是否允许退回至申请人
    public List<String> deptIds; // 已部署的部门信息

    public ProcessDefinitionDetailVO() {

    }

    public ProcessDefinitionDetailVO(ProcessDefinition definition, List<String> deptIds) {
        this.id = definition.id;
        this.name = definition.name;
        this.seqNo = definition.seqNo;
        this.icon = definition.icon;
        this.description = definition.description;
        this.status = definition.status;
        this.ruId = definition.ruId;
        this.formId = definition.formId;
        this.creator = definition.creator;
        this.groupName = definition.groupName;
        this.managers = definition.managers;
        this.deptIds = deptIds;
        this.returnable = definition.returnable;
        this.fallback = definition.fallback;
    }
}
