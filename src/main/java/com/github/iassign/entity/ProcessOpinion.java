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
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.iassign.enums.TaskOperation;
import lombok.Data;

import java.util.Date;

/**
 * 审批意见表
 */
@Data
@TableName("t_process_opinion")
public class ProcessOpinion {
    @TableId(type = IdType.ASSIGN_ID)
    public String id;
    public String instanceId; // 关联的流程实例ID
    public String taskId; // 关联的任务ID
    public String userId;
    public String username;
    public String avatar;
    public String email;
    public String attachments;
    public String remark; // 审批意见
    public Date createTime; // 创建时间
    public TaskOperation operation; // 操作标志位 0-否决 1-同意 2-退回 3-指派
    public String assignId; // 被指派人ID
    public String assignName; // 被指派人姓名
    public String assignAvatar; // 被指派人头像
    public String assignMail; // 被指派人邮箱
}
