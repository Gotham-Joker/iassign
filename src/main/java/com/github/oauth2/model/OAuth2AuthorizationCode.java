package com.github.oauth2.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName( "oauth2_authorization_code")
public class OAuth2AuthorizationCode {

    @TableId
    private String code;
    private String userId;
    private String clientId;
    private String redirectUri;
    private String state;
    private Date createTime;

}
