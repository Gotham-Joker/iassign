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

package com.github.authorization;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * accessToken 凭证，包含用户的权限信息和基本信息
 */
public class AccessTokenAuthentication implements Authentication {
    @Getter
    @Setter
    private String id;
    @Getter
    @Setter
    private String credentials;
    @Getter
    @Setter
    private Set<String> permissions;

    @Setter
    private Boolean admin;
    @Setter
    @Getter
    private UserDetails details;

    public AccessTokenAuthentication() {

    }

    public AccessTokenAuthentication(String id) {
        this.id = id;
    }

    @Override
    public String getCredentials() {
        return credentials;
    }

    @Override
    public boolean isAdmin() {
        return Boolean.TRUE.equals(this.admin);
    }

}
