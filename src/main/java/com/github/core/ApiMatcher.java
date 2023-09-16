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
