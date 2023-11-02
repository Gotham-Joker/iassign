package com.github.oauth2.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName( "oauth2_access_token")
public class OAuth2AccessToken {
    @TableId
    private String accessToken;
    private String userId;
    private String clientId;
    private Long expireIn;
    private String refreshToken;
    private Date refreshTokenCreateTime;
    private Date accessTokenCreateTime;

    public boolean isRefreshTokenExpired() {
        if (System.currentTimeMillis() - refreshTokenCreateTime.getTime() > 7 * 24 * 3600 * 1000) {
            return true;
        }
        return false;
    }

    public boolean isAccessTokenExpired() {
        if (System.currentTimeMillis() - accessTokenCreateTime.getTime() > expireIn * 1000) {
            return true;
        }
        return false;
    }
}
