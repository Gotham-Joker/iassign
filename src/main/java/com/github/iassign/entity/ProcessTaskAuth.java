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
    public Integer type;

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
