package com.github.oauth2.vo;

import lombok.Data;

@Data
public class OAuth2AccessTokenResponse {
    private String accessToken;
    private Long expireIn;

    public OAuth2AccessTokenResponse() {
    }

    public OAuth2AccessTokenResponse(String accessToken,Long expireIn) {
        this.accessToken = accessToken;
        this.expireIn = expireIn;
    }
}
