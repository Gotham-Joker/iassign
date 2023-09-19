package com.github.iassign.controller;

import com.github.iassign.entity.SysMessage;
import com.github.iassign.service.*;
import com.github.authorization.AuthenticationContext;
import com.github.authorization.UserDetails;
import com.github.core.Result;
import com.github.iassign.vo.LoginRequest;
import com.github.iassign.vo.MenuTree;
import com.github.iassign.entity.SysPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class SysController {

    @Autowired
    SysUserService sysUserService;
    @Autowired
    SysMenuService sysMenuService;
    @Autowired
    SysPermissionService sysPermissionService;
    @Autowired
    SysMessageService sysMessageService;
    @Autowired
    LoginService loginService;

    /**
     * 获取用户信息
     */
    @GetMapping("api/user-info")
    public Result userInfo() {
        return Result.success(AuthenticationContext.details());
    }

    /**
     * 获取用户菜单信息
     *
     * @return
     */
    @GetMapping("api/user-menus")
    public Result menus() {
        List<MenuTree> menus = sysMenuService.selectUserMenus();
        return Result.success(menus);
    }

    @PostMapping("api/user-avatar")
    public Result uploadAvatar(@RequestParam MultipartFile file) throws IOException {
        return Result.success(sysUserService.uploadAvatar(file));
    }

    @GetMapping("api/user-permissions")
    public Result userPermissions() {
        String userId = AuthenticationContext.current().getId();
        return Result.success(sysPermissionService.selectBySysUserId(userId).stream().map(SysPermission::getMark).collect(Collectors.toSet()));
    }

    /**
     * 注册sse消息
     */
    @GetMapping(value = "message/{token}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter registerMessage(@PathVariable String token) {
        return sysMessageService.connect(token);
    }

    /**
     * 发送站内消息
     *
     * @param sysMessage 站内信
     */
    @PostMapping("api/message")
    public Result sendMessage(@RequestBody @Validated List<SysMessage> sysMessage) {
        UserDetails details = AuthenticationContext.current().getDetails();
        sysMessageService.send(sysMessage, details);
        return Result.success();
    }

    @GetMapping("api/message")
    public Result queryMessage(@RequestParam Map<String, String> params) {
        String userId = AuthenticationContext.current().getId();
        // 只能查询自己的站内信
        params.put("toUserId", userId);
        return Result.success(sysMessageService.pageQuery(params));
    }

    /**
     * 标记为已读
     *
     * @param id 不传就是全部
     * @return
     */
    @PutMapping("api/message")
    public Result markAsRead(@RequestParam(required = false) String id) {
        sysMessageService.markAsRead(id);
        return Result.success();
    }

    /**
     * 删除站内信(真删除)
     *
     * @param id
     * @return
     */
    @DeleteMapping("api/message")
    public Result deleteById(@RequestParam String id) {
        sysMessageService.delete(id);
        return Result.success();
    }


    // 登录不需要拦截，所以不用api开头
    @PostMapping("login")
    public Result login(@RequestBody @Validated LoginRequest loginRequest) {
        return loginService.login(loginRequest);
    }

    /**
     * 注销登录
     *
     * @return
     */
    @GetMapping("api/logout")
    public Result logout() {
        loginService.logout();
        return Result.success();
    }


}