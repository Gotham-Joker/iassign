package com.github.iassign.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class SysUserDTO {
    public String id;
    public String username;
    public String email;
    public String avatar;
    public String deptId; // 部门ID
    public Boolean admin = false; // 是否是超级管理员

    @TableField(exist = false) // 不是数据库字段
    private Set<String> roleIds = new HashSet<>();
}
