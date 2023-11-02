package com.github.oauth2.controller;

import com.github.authorization.*;
import com.github.core.Result;
import com.github.iassign.dto.ProcessStartDTO;
import com.github.iassign.dto.SysUserRoleDTO;
import com.github.iassign.entity.SysUser;
import com.github.iassign.service.ProcessService;
import com.github.iassign.service.ProcessTaskService;
import com.github.iassign.service.SysUserService;
import com.github.iassign.vo.ProcessTaskTodoQuery;
import com.github.oauth2.service.token.AuthorizationServerTokenService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * OAuthApi
 * 专门负责开放授权api
 */
@RestController
@RequestMapping("oauth-api")
public class OAuthController {
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private AuthorizationServerTokenService authorizationServerTokenService;
    @Autowired
    private ProcessService processService;
    @Autowired
    private ProcessTaskService processTaskService;

    /**
     * 此处的accessToken是对接了Oauth的服务端的accessToken，详情请见AuthorizationCodeController
     *
     * @param accessToken
     * @return
     */
    @GetMapping("user-info")

    public Result<SysUser> userInfo(@RequestParam String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return Result.error(401, "token is required");
        }
        Oauth2Authentication authentication = authorizationServerTokenService.parseAccessToken(accessToken);
        return Result.success(sysUserService.selectById(authentication.getId()));
    }

    private Result convertToAccessTokenAuthentication(String accessToken) {
        // 设置当前登录用户
        Result<SysUser> result = userInfo(accessToken);
        if (result.code != 0) {
            return result;
        }
        SysUser sysUser = result.data;
        UserDetails userDetails = new UserDetails();
        BeanUtils.copyProperties(sysUser, userDetails);
        AccessTokenAuthentication authentication = new AccessTokenAuthentication(userDetails.id);
        authentication.setDetails(userDetails);
        authentication.setAdmin(sysUser.admin);
        return Result.success(authentication);
    }

    /**
     * 启动流程实例
     */
    @PostMapping("process")
    public Result startInstance(@RequestParam String accessToken,
                                @Validated @RequestBody ProcessStartDTO dto) throws Exception {
        try {
            Result result = convertToAccessTokenAuthentication(accessToken);
            if (result.code != 0) {
                return result;
            }
            AuthenticationContext.setAuthentication((Authentication) result.data);
            return Result.success(processService.startInstance(dto).id);
        } finally {
            // 清除当前登录用户
            AuthenticationContext.clearContext();
        }
    }

    /**
     * 查找某个角色下面有哪些用户
     */
    @GetMapping("role-users")
    public Result selectByUserRole(@RequestParam String accessToken,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size,
                                   SysUserRoleDTO dto) {
        Result result = convertToAccessTokenAuthentication(accessToken);
        if (result.code != 0) {
            return result;
        }
        return Result.success(sysUserService.selectByUserRole(page, size, dto));
    }

    /**
     * 查询待办事项
     */
    @GetMapping("todo-list")
    public Result queryTodoList(@RequestParam String accessToken,
                                @RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                ProcessTaskTodoQuery processTaskTodoQuery) {
        try {
            Result result = convertToAccessTokenAuthentication(accessToken);
            if (result.code != 0) {
                return result;
            }
            AuthenticationContext.setAuthentication((Authentication) result.data);
            return Result.success(processTaskService.queryTodoList(page, size, processTaskTodoQuery));
        } finally {
            // 清除当前登录用户
            AuthenticationContext.clearContext();
        }
    }

}
