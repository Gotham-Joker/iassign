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
import lombok.Data;

@Data
@TableName("t_process_task_auth")
public class ProcessTaskAuth {
    @TableId(type = IdType.ASSIGN_ID)
    public String id;
    public String taskId;
    public String referenceId;
    public String name;
    public String avatar;
    public Integer type; // 参考ID的类型 0-用户 1-角色

    public ProcessTaskAuth() {

    }

    public ProcessTaskAuth(SysUser sysUser, String taskId) {
        this.referenceId = sysUser.id;
        this.avatar = sysUser.avatar;
        this.type = 0;
        this.taskId = taskId;
        this.name = sysUser.username;
    }

    public ProcessTaskAuth(SysRole sysRole, String taskId) {
        this.referenceId = sysRole.id;
        this.type = 1;
        this.taskId = taskId;
        this.name = sysRole.name;
    }
}
