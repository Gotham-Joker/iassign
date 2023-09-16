package com.github.authorization;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * accessToken 凭证，包含用户的权限信息和基本信息
 */
public class AccessTokenAuthentication implements Authentication {
    @Getter
    @Setter
    private String id;
    @Getter
    @Setter
    private String credentials;
    @Getter
    @Setter
    private Set<String> permissions;

    @Setter
    private Boolean admin;
    @Setter
    @Getter
    private UserDetails details;

    public AccessTokenAuthentication() {

    }

    public AccessTokenAuthentication(String id) {
        this.id = id;
    }

    @Override
    public String getCredentials() {
        return credentials;
    }

    @Override
    public boolean isAdmin() {
        return Boolean.TRUE.equals(this.admin);
    }

}
