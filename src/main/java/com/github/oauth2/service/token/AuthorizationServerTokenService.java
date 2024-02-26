/*
 * MIT License
 *
 * Copyright (c) 2024 Hongtao Liu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.oauth2.service.token;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.authorization.AccessTokenAuthentication;
import com.github.authorization.Authentication;
import com.github.authorization.Oauth2Authentication;
import com.github.authorization.UserDetails;
import com.github.authorization.exception.InvalidGrantException;
import com.github.iassign.entity.SysUser;
import com.github.iassign.service.AccessTokenService;
import com.github.iassign.service.SysUserService;
import com.github.oauth2.mapper.AuthorizationServerTokenMapper;
import com.github.oauth2.model.OAuth2AccessToken;
import com.github.oauth2.model.OAuth2AuthorizationCode;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthorizationServerTokenService {
    @Autowired
    private AuthorizationServerTokenMapper authorizationServerTokenMapper;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private AccessTokenService accessTokenService;

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

    public Authentication parseAccessToken(String accessToken) {
        OAuth2AccessToken oAuth2AccessToken = Optional.ofNullable(authorizationServerTokenMapper.selectById(accessToken))
                .orElseThrow(() -> new InvalidGrantException("Invalid access token"));
        if (oAuth2AccessToken.isAccessTokenExpired()) {
            throw new InvalidGrantException("access token expired");
        }
        String userId = oAuth2AccessToken.getUserId();
        SysUser sysUser = sysUserService.selectById(userId);
        UserDetails userDetails = new UserDetails();
        BeanUtils.copyProperties(sysUser, userDetails);
        AccessTokenAuthentication authentication = new AccessTokenAuthentication(userDetails.id);
        String cachedToken = accessTokenService.retrieveByUserId(userId);
        if (!StringUtils.hasText(accessToken)) {
            cachedToken = accessTokenService.createAccessToken(userDetails).getAccessToken();
        }
        authentication.setDetails(userDetails);
        authentication.setAdmin(sysUser.admin);
        authentication.setCredentials(cachedToken);
        return authentication;
    }
}
