package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
public class SysUser {
    @TableId
    public String id;
    public String username;
    public String email;
    public String avatar;
    public String deptId; // 部门ID
    public String deptCode; // 部门代码
    public String deptName; // 部门名称(繁体)
    public String fovaAo;
    public String stockAo;
    public Boolean admin = false; // 是否是超级管理员
    public Date createTime;
    public Date updateTime;
    // 员工号
    public String staffId;

    @TableField(exist = false) // 不是数据库字段
    private Set<String> roleIds = new HashSet<>();
}
