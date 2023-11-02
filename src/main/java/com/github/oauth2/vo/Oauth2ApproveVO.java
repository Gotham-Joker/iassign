package com.github.oauth2.vo;

import lombok.Data;

import java.util.Set;

@Data
public class Oauth2ApproveVO {
    public Set<String> scopes;
    public String state;
}
