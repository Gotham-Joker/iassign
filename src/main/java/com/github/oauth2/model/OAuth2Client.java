package com.github.oauth2.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName( "oauth2_client")
public class OAuth2Client {
    @TableId
    public String clientId;
    public String clientSecret;
    public String clientName;
    public String redirectUri;
    public String mark;
    public String ownerId;
}
