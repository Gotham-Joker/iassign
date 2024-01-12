package com.github.iassign.vo;

import com.github.iassign.entity.ProcessTaskAuth;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class TaskAuthVO {
    public Boolean actionable = false; // 可操作的
    public Set<ProcessTaskAuth> users = new HashSet<>(); // 可审批角色ID
    public Set<ProcessTaskAuth> roles = new HashSet<>(); // 可审批用户ID
}
