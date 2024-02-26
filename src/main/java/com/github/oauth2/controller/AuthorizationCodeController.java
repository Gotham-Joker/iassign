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

package com.github.oauth2.controller;

import com.github.authorization.exception.OAuth2Exception;
import com.github.iassign.entity.SysUser;
import com.github.iassign.service.SysUserService;
import com.github.oauth2.model.OAuth2AccessToken;
import com.github.oauth2.service.client.OAuth2ClientService;
import com.github.authorization.AuthenticationContext;
import com.github.core.Result;
import com.github.oauth2.model.OAuth2AuthorizationCode;
import com.github.oauth2.model.OAuth2Client;
import com.github.oauth2.service.code.OAuth2AuthorizationCodeService;
import com.github.oauth2.service.token.AuthorizationServerTokenService;
import com.github.oauth2.vo.OAuth2AuthenticationRequest;
import com.github.oauth2.vo.Oauth2ApproveVO;
import com.github.oauth2.vo.OauthTokenInternalVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * oauth2.0授权码模式
 */
@Controller
public class AuthorizationCodeController {
    @Autowired
    private OAuth2AuthorizationCodeService authorizationCodeService;
    @Autowired
    private OAuth2ClientService oauth2ClientService;
    @Autowired
    private AuthorizationServerTokenService authorizationServerTokenService;
    @Autowired
    private SysUserService sysUserService;
    @Value("${iassign.oauth.web:}")
    String oauthWebUrl;

    /**
     * 请求授权码
     *
     * @param clientId
     * @param state
     * @param redirectUri
     * @return
     */
    @RequestMapping(path = "/oauth/authorize")
    public ModelAndView authorize(@RequestParam("client_id") String clientId,
                                  @RequestParam(required = false) String state,
                                  @RequestParam("redirect_uri") String redirectUri) {
        // 判断clientId是否存在
        oauth2ClientService.checkRequest(clientId);
        // 封装请求
        OAuth2AuthenticationRequest request = new OAuth2AuthenticationRequest(clientId, redirectUri, state, System.currentTimeMillis());
        // 授权类型校验,client校验
        String reqInfo = oauth2ClientService.createState(request);
        return new ModelAndView("redirect:" + oauthWebUrl + "/index.html#/passport/login?state="
                + reqInfo + "&clientId=" + clientId);
    }

    /**
     * 用户授权，需要先登录，所以需要/api开头
     *
     * @return
     */
    @PostMapping(path = "/api/oauth-approve")
    @ResponseBody
    public Result approveOrDeny(@RequestBody Oauth2ApproveVO vo) {
        if (!StringUtils.hasText(vo.state)) {
            return Result.success();
        }
        OAuth2AuthenticationRequest request = oauth2ClientService.parseState(vo.state);
        if (request == null) {
            throw new OAuth2Exception(500, "Cannot approve uninitialized authorization request.");
        }
        oauth2ClientService.checkRequest(request.getClientId());
        String userId = AuthenticationContext.current().getId();
        if (!StringUtils.hasText(userId)) {
            return Result.error(401, "please login fist");
        }
        String code = authorizationCodeService.createAuthorizationCode(userId, request);
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(request.getRedirectUri()).append("?code=").append(code);
        if (StringUtils.hasText(request.getState())) {
            urlBuilder.append("&state=").append(request.getState());
        }
        return Result.success(urlBuilder.toString());
    }

    /**
     * 申请access_token，这种需要用户确认，不太信任第三方对接
     *
     * @param parameters
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/oauth/token")
    public Result<OAuth2AccessToken> applyForAccessToken(@RequestParam Map<String, String> parameters) {
        String clientId = parameters.get("client_id");
        String clientSecret = parameters.get("client_secret");
        String code = parameters.get("code");
        if (!StringUtils.hasText(code)) {
            throw new OAuth2Exception(422, "code can not be empty");
        }
        OAuth2Client client = oauth2ClientService.checkRequest(clientId);
        if (!client.getClientSecret().equals(clientSecret)) {
            throw new OAuth2Exception(401, "bad client secret");
        }
        OAuth2AuthorizationCode authorizationCode = authorizationCodeService.consumeAuthorizationCode(code);
        if (!authorizationCode.getClientId().equals(clientId)) {
            throw new OAuth2Exception(500, "provided clientId mismatch current clientId");
        }
        OAuth2AccessToken accessToken = authorizationServerTokenService.createAccessToken(authorizationCode);
        return Result.success(accessToken);
    }

    /**
     * 申请accessToken内部版，内部应用对接，不需要用户确认，但是必须事先注册client id
     */
    @ResponseBody
    @PostMapping(value = "/oauth/token/internal")
    public Result<String> applyAccessTokenInternal(@RequestBody OauthTokenInternalVO vo) {
        String clientId = vo.clientId;
        String clientSecret = vo.clientSecret;
        String userId = vo.userId;
        if (!StringUtils.hasText(clientId)) {
            return Result.error(500, "client id is required");
        }
        OAuth2Client client = oauth2ClientService.checkRequest(clientId);
        if (!client.getClientSecret().equals(clientSecret)) {
            return Result.error(500, "bad client secret");
        }
        if (!StringUtils.hasText(userId)) {
            return Result.error(500, "userId is required");
        }
        SysUser sysUser = sysUserService.selectById(userId);
        if (sysUser == null) {
            return Result.error(500, "userId is invalid");
        }
        OAuth2AuthorizationCode authorizationCode = new OAuth2AuthorizationCode();
        authorizationCode.setUserId(userId);
        authorizationCode.setClientId(clientId);
        OAuth2AccessToken accessToken = authorizationServerTokenService.createAccessToken(authorizationCode);
        return Result.success(accessToken.getAccessToken());
    }

    /**
     * refresh_token
     *
     * @param parameters
     * @return
     *//*
    @RequestMapping(value = "/oauth/token", method = RequestMethod.POST, params = {"grant_type=refresh_token"})
    @ResponseBody
    public Result refreshAccessToken(@RequestParam Map<String, String> parameters) {
        String refreshToken = parameters.get("refresh_token");
        String clientId = parameters.get("client_id");
        String clientSecret = parameters.get("client_secret");
        // 校验客户端id和密码
        OAuth2Client client = oauth2ClientService.checkRequest(clientId);
        if (client.getClientSecret().equals(clientSecret)) {
            throw new OAuth2Exception(401, "bad client secret");
        }
        OAuth2AccessToken accessToken = authorizationServerTokenService.refreshAccessToken(refreshToken);
        if (!accessToken.getClientId().equals(clientId)) {
            throw new OAuth2Exception(500, "invalid client id for this refresh token");
        }
        return Result.success(accessToken);
    }*/


}
