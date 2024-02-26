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

package com.github.oauth2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.iassign.service.SysUserService;
import com.github.oauth2.authorization.OAuthFilter;
import com.github.oauth2.service.token.AuthorizationServerTokenService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OAuthTokenFilterConfig {

    // 注册accessToken过滤器
    @Bean
    public FilterRegistrationBean oauthFilter(ObjectMapper objectMapper,
                                              AuthorizationServerTokenService accessTokenService) {
        FilterRegistrationBean<OAuthFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new OAuthFilter(objectMapper, accessTokenService));
        bean.addUrlPatterns("/oauth-api/*");
        bean.setOrder(11);
        return bean;
    }
}
