package com.github.authorization;

import lombok.Data;

@Data
public class UserDetails {
    public String id;   // 用户在本系统中的唯一ID
    public String username; // 用户名
    public String email;
    public String avatar;
    public String deptId; // 部门ID
    public String deptCode; // 部门代码
    public String deptName; // 部门名称(繁体)
    public Boolean admin;
}
