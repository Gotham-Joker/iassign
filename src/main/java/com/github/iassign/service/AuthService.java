package com.github.iassign.service;

import com.github.authorization.Authentication;
import com.github.authorization.AuthenticationContext;
import com.github.core.ApiMatcher;
import com.github.core.ApiPathContainer;
import com.github.core.Result;
import com.github.iassign.entity.SysPermission;
import com.github.iassign.event.PmChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 鉴权服务，用来判断用户是否有权限访问某个url，
 * 支持AntPathMater
 * 例如: 用户管理 /api/user/** 或/api/user/{id:\\d+}
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private SysPermissionService sysPermissionService;

    // 路径直接匹配
    private static final Map<String, List<ApiMatcher>> directPathPermissions = new ConcurrentHashMap<>();
    // 含有表达式的匹配器,如: user/{id:d\+},或list/index*.html
    // 注意线程安全问题
    private static final Set<ApiMatcher> patternPathPermissions = Collections.synchronizedSet(new HashSet<>());

    // 将系统所有敏感url，加载到内存
    @PostConstruct
    public void loadAll() {
        List<SysPermission> permissions = sysPermissionService.findAll();
        if (permissions != null) {
            for (SysPermission permission : permissions) {
                addMatcher(permission);
            }
        }
    }

    // 从消息队列消费一条权限变更事件
    // 管理内存中的权限
    public void receive(PmChangeEvent event) {
        SysPermission newPermission = event.getNewPermission();
        SysPermission oldPermission = event.getOldPermission();
        if (oldPermission != null) {
            ApiMatcher matcher = new ApiMatcher(oldPermission.mark, oldPermission.url, oldPermission.method);
            List<ApiMatcher> matchers = directPathPermissions.get(oldPermission.url);
            if (matchers != null) {
                for (int i = matchers.size() - 1; i >= 0; i--) {
                    if (matchers.get(i).equals(matcher)) {
                        matchers.remove(i);
                    }
                }
            }
            if (CollectionUtils.isEmpty(matchers)) {
                directPathPermissions.remove(oldPermission.url);
            }
            patternPathPermissions.remove(matcher);
        }
        if (newPermission != null) {
            addMatcher(newPermission);
        }
    }

    private void addMatcher(SysPermission permission) {
        ApiMatcher matcher = new ApiMatcher(permission.mark, permission.url, permission.method);
        if (!matcher.pathPattern.hasPatternSyntax()) {
            directPathPermissions.computeIfAbsent(matcher.pathPattern.getPatternString(), (url) -> new ArrayList<>())
                    .add(matcher);
        } else {
            patternPathPermissions.add(matcher);
        }
    }

    /**
     * 用户权限决策
     *
     * @param path
     * @param method http请求方法，大写，eg: GET
     * @return
     */
    public Result decide(String path, String method) {

        Authentication authentication = AuthenticationContext.current();
        if (authentication.isAdmin()) {
            return Result.success(); // 管理员不适用此原则，直接放行
        }
        Set<String> userPermissionMarks = authentication.getPermissions();

        List<ApiMatcher> matchers = directPathPermissions.get(path);
        if (!CollectionUtils.isEmpty(matchers)) { // 用户访问了敏感资源(url直接匹配)
            for (int i = 0; i < matchers.size(); i++) {
                ApiMatcher matcher = matchers.get(i);
                if (matcher.method == null || method.equals(matcher.method)) {
                    if (userPermissionMarks.contains(matcher.mark)) {
                        return Result.success(); // 包含权限，放行
                    }
                    // 用户没有该权限，拒绝访问
                    return Result.error(403, "access denied");
                }
            }
        }

        // url表达式匹配
        ApiPathContainer pathContainer = new ApiPathContainer(path);
        Optional<ApiMatcher> matcher = patternPathPermissions.stream().filter(m -> m.pathPattern.matches(pathContainer))
                .min(Comparator.comparing(m -> m.pathPattern));

        if (matcher.isPresent()) { // 用户访问了敏感资源
            if (!userPermissionMarks.contains(matcher.get().mark)) {
                // 用户没有该权限，拒绝访问
                return Result.error(403, "access denied");
            }
        }
        return Result.success();
    }


}
