package com.github.oauth2.vo;

import lombok.Data;

@Data
public class OauthTokenInternalVO {
    public String userId;
    public String clientId;
    public String clientSecret;
}
