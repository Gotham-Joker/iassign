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

package com.github.iassign.service;

import com.github.authorization.*;
import com.github.authorization.exception.AuthenticationException;
import com.github.iassign.entity.SysPermission;
import com.github.authorization.constant.AuthorizationConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用于创建和销毁accessToken
 */
@Service
public class AccessTokenService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SysPermissionService sysPermissionService;

    // token有效期为一天
    @Value("${iassign.security.token-ttl:86400}")
    private Long tokenTtl;

    /**
     * 根据用户提供的token转换成authentication
     *
     * @param accessToken
     * @return
     */
    public Authentication parse(String accessToken) throws AuthenticationException {
        if (!StringUtils.hasText(accessToken)) {
            throw new AuthenticationException("token is required");
        }
        // 查找用户信息
        AccessTokenAuthentication authentication = (AccessTokenAuthentication) redisTemplate.boundValueOps(AuthorizationConstant.USER_INFO_KEY_PREFIX + accessToken).get();
        if (authentication == null) {
            throw new AuthenticationException("令牌已过期，请重新登录");
        }
        // 查找权限信息
        Set<String> permissionMark = (Set<String>) redisTemplate.boundValueOps(AuthorizationConstant.USER_PERMISSION_KEY_PREFIX + authentication.getId()).get();
        authentication.setPermissions(permissionMark);
        authentication.setCredentials(accessToken); // 当前token也放进去
        return authentication;
    }

    /**
     * 创建accessToken
     */
    public AccessToken createAccessToken(UserDetails userDetails) {
        BoundValueOperations<String, String> tokenOps = stringRedisTemplate.boundValueOps(AuthorizationConstant.ACCESS_TOKEN_KEY_PREFIX + userDetails.getId());
        //重复登录会导致前一个token失效
        String accessToken = tokenOps.get();
        if (StringUtils.hasText(accessToken)) {
            redisTemplate.delete(AuthorizationConstant.USER_INFO_KEY_PREFIX + accessToken);
        }

        // 生成accessToken并缓存
        accessToken = UUID.randomUUID().toString().replaceAll("-", "");
        tokenOps.set(accessToken, tokenTtl, TimeUnit.SECONDS);

        // 构造认证对象并缓存
        AccessTokenAuthentication authentication = new AccessTokenAuthentication(userDetails.getId());
        authentication.setAdmin(userDetails.admin);
        authentication.setDetails(userDetails);
        redisTemplate.boundValueOps(AuthorizationConstant.USER_INFO_KEY_PREFIX + accessToken).set(authentication, tokenTtl, TimeUnit.SECONDS);

        // 缓存用户权限(标识)
        Set<String> permissions = sysPermissionService.selectBySysUserId(userDetails.getId()).stream()
                .map(SysPermission::getMark).collect(Collectors.toSet());
        redisTemplate.boundValueOps(AuthorizationConstant.USER_PERMISSION_KEY_PREFIX + userDetails.getId()).set(permissions, tokenTtl, TimeUnit.SECONDS);

        return new AccessToken(accessToken, tokenTtl);
    }

    public void eraseAccessToken() {
        String userId = AuthenticationContext.current().getId();
        String accessToken = AuthenticationContext.current().getCredentials();
        stringRedisTemplate.delete(AuthorizationConstant.ACCESS_TOKEN_KEY_PREFIX + userId);
        redisTemplate.delete(AuthorizationConstant.USER_INFO_KEY_PREFIX + accessToken);
        redisTemplate.delete(AuthorizationConstant.USER_PERMISSION_KEY_PREFIX + userId);
    }

    public String retrieveByUserId(String userId) {
        BoundValueOperations<String, String> tokenOps = stringRedisTemplate.boundValueOps(AuthorizationConstant.ACCESS_TOKEN_KEY_PREFIX + userId);
        //重复登录会导致前一个token失效
        return tokenOps.get();
    }
}
