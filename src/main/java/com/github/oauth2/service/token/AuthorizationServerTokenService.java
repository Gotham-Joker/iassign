package com.github.oauth2.service.token;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.authorization.Oauth2Authentication;
import com.github.authorization.exception.InvalidGrantException;
import com.github.oauth2.mapper.AuthorizationServerTokenMapper;
import com.github.oauth2.model.OAuth2AccessToken;
import com.github.oauth2.model.OAuth2AuthorizationCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthorizationServerTokenService {
    @Autowired
    private AuthorizationServerTokenMapper authorizationServerTokenMapper;

    public OAuth2AccessToken createAccessToken(OAuth2AuthorizationCode authorizationCode) {
        // 每次申请token都会使上一次申请的token失效（直接删掉吧）
        authorizationServerTokenMapper.deleteByClientIdAndUserId(authorizationCode.getClientId(), authorizationCode.getUserId());
        //生成新的token
        OAuth2AccessToken accessToken = new OAuth2AccessToken();
        accessToken.setAccessToken(UUID.randomUUID().toString().replaceAll("-", ""));
        accessToken.setRefreshToken(UUID.randomUUID().toString().replaceAll("-", ""));
        Date date = new Date();
        // refresh token的创建时间
        accessToken.setRefreshTokenCreateTime(date);
        // access token的创建时间
        accessToken.setAccessTokenCreateTime(date);
        accessToken.setExpireIn(7200L);
        accessToken.setUserId(authorizationCode.getUserId());
        accessToken.setClientId(authorizationCode.getClientId());
        authorizationServerTokenMapper.insert(accessToken);
        return accessToken;
    }

    public OAuth2AccessToken refreshAccessToken(String refreshToken) {
        QueryWrapper<OAuth2AccessToken> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refresh_token", refreshToken);
        OAuth2AccessToken accessToken = authorizationServerTokenMapper.selectOne(queryWrapper);
        if (accessToken == null) {
            throw new InvalidGrantException("Invalid refresh token");
        }
        if (accessToken.isRefreshTokenExpired()) {
            authorizationServerTokenMapper.delete(queryWrapper);
            throw new InvalidGrantException("refresh token expired");
        }
        // 旧的refresh token继续使用，这里只做access token的生成
        String newAccessToken = UUID.randomUUID().toString().replaceAll("-", "");
        accessToken.setAccessToken(newAccessToken);
        accessToken.setAccessTokenCreateTime(new Date());
        // 更新access token
        authorizationServerTokenMapper.insert(accessToken);
        return accessToken;
    }

    public Oauth2Authentication parseAccessToken(String accessToken) {
        OAuth2AccessToken oAuth2AccessToken = Optional.ofNullable(authorizationServerTokenMapper.selectById(accessToken))
                .orElseThrow(() -> new InvalidGrantException("Invalid access token"));
        if (oAuth2AccessToken.isAccessTokenExpired()) {
            throw new InvalidGrantException("access token expired");
        }
        String userId = oAuth2AccessToken.getUserId();
        String clientId = oAuth2AccessToken.getClientId();
        return new Oauth2Authentication(userId, clientId);
    }
}
