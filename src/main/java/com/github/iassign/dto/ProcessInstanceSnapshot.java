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

import com.github.iassign.entity.ProcessInstance;
import com.github.iassign.enums.ProcessInstanceStatus;
import lombok.Data;

import java.util.Date;

/**
 * 流程实例快照
 */
@Data
public class ProcessInstanceSnapshot {
    public String id;
    public String definitionId; // 流程定义ID
    public String name; // 流程实例名
    public String starter; // 发起人ID
    public String starterName; // 发起人姓名
    public String deptId; // 发起人所在部门ID
    public String handlerId; // 当前处理人用户ID
    public String handlerName; // 当前处理人用户名
    public String preHandlerId; // 上一处理人用户ID
    public Date createTime; // 申请日期
    public ProcessInstanceStatus status; // 0-已撤回 1-执行中 2-成功 3-失败

    public ProcessInstanceSnapshot() {

    }

    public ProcessInstanceSnapshot(ProcessInstance instance) {
        this.id = instance.id;
        this.definitionId = instance.definitionId;
        this.name = instance.name;
        this.starter = instance.starter;
        this.starterName = instance.starterName;
        this.handlerId = instance.handlerId;
        this.handlerName = instance.handlerName;
        this.preHandlerId = instance.preHandlerId;
        this.status = instance.status;
        this.deptId = instance.deptId;
        this.createTime = instance.createTime;
    }
}
