package com.github.authorization;

import java.util.Set;

public interface Authentication {
    String getId();

    String getCredentials();

    boolean isAdmin(); // 是否超级管理员

    Set<String> getPermissions();

    UserDetails getDetails(); // 用户更详细的信息

}
