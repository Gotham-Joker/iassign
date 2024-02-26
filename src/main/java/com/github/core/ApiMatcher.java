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

package com.github.core;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

public class ApiMatcher {
    public final String mark; // 权限唯一标识,因为要常驻内存，所以建议尽量短一些
    public final String method;
    public final PathPattern pathPattern;

    public ApiMatcher(String mark, String url, String method) {
        Assert.hasText(mark, "权限标识不能为空");
        Assert.hasText(url, "url不能为空");
        this.mark = mark;
        this.method = StringUtils.hasText(method) ? method : null;
        this.pathPattern = PathPatternParser.defaultInstance.parse(url);
    }

    @Override
    public int hashCode() {
        return pathPattern.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ApiMatcher)) {
            return false;
        }
        ApiMatcher other = (ApiMatcher) obj;
        if (!mark.equals(other.mark)) {
            return false;
        }
        if (!pathPattern.equals(other.pathPattern)) {
            return false;
        }
        if (method != null) {
            if (method.equals(other.method)) {
                return true;
            }
        } else {
            if (other.method == null) {
                return true;
            }
        }
        return false;
    }
}
