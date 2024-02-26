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

package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.iassign.enums.ProcessTaskStatus;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@TableName("t_process_task")
public class ProcessTask {
    @TableId(type = IdType.ASSIGN_ID)
    public String id;
    public String definitionId;
    public String instanceId;
    public String formId;
    public String variableId;  // 临时变量,仅对后续非用户审批环节生效
    public String formInstanceId; // 提交的表单ID 会签的时候，共享同一个表单，即：多个人填写同一个表单
    public String name; // 任务名称
    public String handlerId; // 受理人的ID
    public String preHandlerId; // 上一受理人的ID
    public String incomeId; // 进入当前DAG节点的路线ID
    public String dagNodeId; // 当前DAG节点ID

    public Boolean countersign; // 是否是会签节点 0-否 1-是
    public Boolean fileRequired; // 必须上传附件
    public Boolean assign; // 指派标志位： 是否可以指派 0-不可以 1-可以
    public Boolean userNode; // 是否是用户可以审批的环节(有些环节是不能审批的，是系统自动处理的)
    public ProcessTaskStatus status; // 状态
    public Date createTime;  // 开始时间
    public Date updateTime; // 完成时间或退回时间

    // 审批意见
    @TableField(exist = false)
    public List<ProcessOpinion> opinions;
}
